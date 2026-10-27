package com.tdb.vazhipadu.service;

import com.tdb.vazhipadu.dto.request.OrderItemRequest;
import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.exception.ErrorCode;
import com.tdb.vazhipadu.repository.OrderItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyLimitService {

    private static final Map<String, Object> LOCK_TIMEOUT = Map.of("jakarta.persistence.lock.timeout", 10_000);

    private final EntityManager entityManager;
    private final OrderItemRepository orderItemRepository;

    public DailyLimitService(EntityManager entityManager, OrderItemRepository orderItemRepository) {
        this.entityManager = entityManager;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void ensureCapacity(LocalDate offeringDate, List<OrderItemRequest> items, Map<Long, Offering> offerings) {
        items.stream()
                .filter(item -> offerings.get(item.offeringId()).hasDailyLimit())
                .sorted(Comparator.comparing(OrderItemRequest::offeringId))
                .forEach(item -> reserve(offerings.get(item.offeringId()), offeringDate, item.quantity()));
    }

    private void reserve(Offering offering, LocalDate offeringDate, int quantity) {
        entityManager.refresh(offering, LockModeType.PESSIMISTIC_WRITE, LOCK_TIMEOUT);
        if (!offering.isActive()) {
            throw new ApiException(
                    ErrorCode.OFFERING_INACTIVE, "Offering '%s' is not currently active".formatted(offering.getName()));
        }
        if (!offering.hasDailyLimit()) {
            return;
        }
        var booked = orderItemRepository.sumQuantity(offering.getId(), offeringDate, OrderStatus.CONFIRMED);
        var limit = offering.getDailyLimit();
        if (booked + quantity > limit) {
            throw new ApiException(
                    ErrorCode.DAILY_LIMIT_EXCEEDED,
                    "'%s' has %d of %d slots left on %s"
                            .formatted(offering.getName(), Math.max(0, limit - booked), limit, offeringDate));
        }
    }
}
