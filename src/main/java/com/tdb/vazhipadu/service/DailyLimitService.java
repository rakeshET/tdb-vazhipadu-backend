package com.tdb.vazhipadu.service;

import com.tdb.vazhipadu.dto.request.OrderItemRequest;
import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.exception.ErrorCode;
import com.tdb.vazhipadu.repository.OfferingRepository;
import com.tdb.vazhipadu.repository.OrderItemRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyLimitService {

    private final OfferingRepository offeringRepository;
    private final OrderItemRepository orderItemRepository;

    public DailyLimitService(OfferingRepository offeringRepository, OrderItemRepository orderItemRepository) {
        this.offeringRepository = offeringRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void ensureCapacity(LocalDate offeringDate, List<OrderItemRequest> items, Map<Long, Offering> offerings) {
        var limitedIds = items.stream()
                .map(OrderItemRequest::offeringId)
                .filter(id -> offerings.get(id).hasDailyLimit())
                .toList();
        if (limitedIds.isEmpty()) {
            return;
        }
        var requested = items.stream()
                .collect(Collectors.toMap(OrderItemRequest::offeringId, OrderItemRequest::quantity));
        for (var offering : offeringRepository.lockAllByIdInOrderById(limitedIds)) {
            var booked = orderItemRepository.sumQuantity(offering.getId(), offeringDate, OrderStatus.CONFIRMED);
            var limit = offering.getDailyLimit();
            if (booked + requested.get(offering.getId()) > limit) {
                throw new ApiException(ErrorCode.DAILY_LIMIT_EXCEEDED,
                        "'%s' has %d of %d slots left on %s".formatted(
                                offering.getName(), Math.max(0, limit - booked), limit, offeringDate));
            }
        }
    }
}
