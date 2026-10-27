package com.tdb.vazhipadu.service;

import com.tdb.vazhipadu.dto.request.CreateOrderRequest;
import com.tdb.vazhipadu.dto.request.OrderItemRequest;
import com.tdb.vazhipadu.dto.request.OrderSearchCriteria;
import com.tdb.vazhipadu.dto.response.OrderResponse;
import com.tdb.vazhipadu.dto.response.OrderSummaryResponse;
import com.tdb.vazhipadu.dto.response.PageResponse;
import com.tdb.vazhipadu.entity.OfferingOrder;
import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.exception.ErrorCode;
import com.tdb.vazhipadu.mapper.OrderMapper;
import com.tdb.vazhipadu.repository.OfferingRepository;
import com.tdb.vazhipadu.repository.OrderRepository;
import com.tdb.vazhipadu.repository.OrderSpecifications;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OfferingRepository offeringRepository;
    private final TempleService templeService;
    private final DailyLimitService dailyLimitService;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, OfferingRepository offeringRepository,
                        TempleService templeService, DailyLimitService dailyLimitService, Clock clock) {
        this.orderRepository = orderRepository;
        this.offeringRepository = offeringRepository;
        this.templeService = templeService;
        this.dailyLimitService = dailyLimitService;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse placeOrder(Long templeId, CreateOrderRequest request) {
        var temple = templeService.findTempleOrThrow(templeId);
        requireNotInPast(request.offeringDate());
        var offerings = findBookableOfferings(templeId, request.items());
        dailyLimitService.ensureCapacity(request.offeringDate(), request.items(), offerings);

        var order = new OfferingOrder(temple, request.devoteeName().strip(), request.phone(), request.nakshatra(),
                request.offeringDate(), Instant.now(clock));
        request.items().forEach(item -> order.addItem(offerings.get(item.offeringId()), item.quantity()));

        var saved = orderRepository.save(order);
        log.info("Order placed id={} templeId={} total={}", saved.getId(), templeId, saved.getTotalAmount());
        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long templeId, Long orderId) {
        return OrderMapper.toResponse(findOrderOrThrow(templeId, orderId));
    }

    @Transactional
    public OrderResponse cancelOrder(Long templeId, Long orderId) {
        var order = findOrderOrThrow(templeId, orderId);
        if (order.isCancelled()) {
            throw new ApiException(ErrorCode.ORDER_ALREADY_CANCELLED,
                    "Order %d is already cancelled".formatted(orderId));
        }
        if (order.getOfferingDate().isBefore(LocalDate.now(clock))) {
            throw new ApiException(ErrorCode.ORDER_NOT_CANCELLABLE,
                    "Order %d was for a past date and can no longer be cancelled".formatted(orderId));
        }
        order.cancel(Instant.now(clock));
        log.info("Order cancelled id={} templeId={}", orderId, templeId);
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> searchOrders(Long templeId, OrderSearchCriteria criteria,
                                                           int page, int size) {
        templeService.findTempleOrThrow(templeId);
        var pageable = PageRequest.of(page, size, Sort.by("id"));
        var orders = orderRepository.findAll(OrderSpecifications.matching(templeId, criteria), pageable);
        return PageResponse.from(orders.map(OrderMapper::toSummary));
    }

    private OfferingOrder findOrderOrThrow(Long templeId, Long orderId) {
        return orderRepository.findWithItemsByIdAndTempleId(orderId, templeId)
                .orElseThrow(() -> ApiException.notFound("Order", orderId));
    }

    private void requireNotInPast(LocalDate offeringDate) {
        if (offeringDate.isBefore(LocalDate.now(clock))) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "offeringDate must be today or later");
        }
    }

    private Map<Long, Offering> findBookableOfferings(Long templeId, List<OrderItemRequest> items) {
        var ids = items.stream().map(OrderItemRequest::offeringId).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Each offering may appear only once per order");
        }
        var offerings = offeringRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Offering::getId, Function.identity()));
        ids.forEach(id -> requireBookable(offerings.get(id), id, templeId));
        return offerings;
    }

    private static void requireBookable(Offering offering, Long offeringId, Long templeId) {
        if (offering == null || !offering.belongsTo(templeId)) {
            throw new ApiException(ErrorCode.UNKNOWN_OFFERING,
                    "Offering %d is not available at this temple".formatted(offeringId));
        }
        if (!offering.isActive()) {
            throw new ApiException(ErrorCode.OFFERING_INACTIVE,
                    "Offering '%s' is not currently active".formatted(offering.getName()));
        }
    }
}
