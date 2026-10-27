package com.tdb.vazhipadu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdb.vazhipadu.dto.request.OrderItemRequest;
import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.entity.Temple;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.exception.ErrorCode;
import com.tdb.vazhipadu.repository.OrderItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyLimitServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);
    private static final Long POOJA_ID = 3L;

    @Mock
    private EntityManager entityManager;
    @Mock
    private OrderItemRepository orderItemRepository;
    @InjectMocks
    private DailyLimitService dailyLimitService;

    private final Offering pooja = offering(POOJA_ID, "Udayasthamana Pooja", 5);

    @Test
    void shouldRejectOrderThatWouldExceedDailyLimit() {
        givenBooked(POOJA_ID, 4);

        assertThatThrownBy(() -> ensureCapacity(List.of(new OrderItemRequest(POOJA_ID, 2)), pooja))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo(ErrorCode.DAILY_LIMIT_EXCEEDED);
                    assertThat(ex.getMessage()).isEqualTo("'Udayasthamana Pooja' has 1 of 5 slots left on 2026-11-01");
                });
    }

    @Test
    void shouldAllowOrderThatExactlyFillsDailyLimit() {
        givenBooked(POOJA_ID, 4);

        assertThatCode(() -> ensureCapacity(List.of(new OrderItemRequest(POOJA_ID, 1)), pooja))
                .doesNotThrowAnyException();
        verify(entityManager).refresh(eq(pooja), eq(LockModeType.PESSIMISTIC_WRITE), anyMap());
    }

    @Test
    void shouldNotLockOrQueryOfferingsWithoutDailyLimit() {
        var pushpanjali = offering(1L, "Pushpanjali", null);

        ensureCapacity(List.of(new OrderItemRequest(1L, 50)), pushpanjali);

        verifyNoInteractions(entityManager, orderItemRepository);
    }

    @Test
    void shouldLockOnlyLimitedOfferingsInAscendingIdOrder() {
        var pushpanjali = offering(1L, "Pushpanjali", null);
        var thulabharam = offering(9L, "Thulabharam", 10);
        var items = List.of(new OrderItemRequest(9L, 1), new OrderItemRequest(1L, 1), new OrderItemRequest(POOJA_ID, 1));

        dailyLimitService.ensureCapacity(DATE, items,
                Map.of(1L, pushpanjali, 9L, thulabharam, POOJA_ID, pooja));

        var order = inOrder(entityManager);
        order.verify(entityManager).refresh(eq(pooja), eq(LockModeType.PESSIMISTIC_WRITE), anyMap());
        order.verify(entityManager).refresh(eq(thulabharam), eq(LockModeType.PESSIMISTIC_WRITE), anyMap());
        verify(entityManager, never()).refresh(eq(pushpanjali), any(LockModeType.class), anyMap());
    }

    @Test
    void shouldCheckLimitReadUnderLockNotTheStaleValue() {
        doAnswer(inv -> {
            ReflectionTestUtils.setField(pooja, "dailyLimit", 2);
            return null;
        }).when(entityManager).refresh(eq(pooja), eq(LockModeType.PESSIMISTIC_WRITE), anyMap());
        when(orderItemRepository.sumQuantity(POOJA_ID, DATE, OrderStatus.CONFIRMED)).thenReturn(2L);

        assertThatThrownBy(() -> ensureCapacity(List.of(new OrderItemRequest(POOJA_ID, 1)), pooja))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo(ErrorCode.DAILY_LIMIT_EXCEEDED));
    }

    @Test
    void shouldRejectOfferingDeactivatedBeforeLockWasAcquired() {
        doAnswer(inv -> {
            ReflectionTestUtils.setField(pooja, "active", false);
            return null;
        }).when(entityManager).refresh(eq(pooja), eq(LockModeType.PESSIMISTIC_WRITE), anyMap());

        assertThatThrownBy(() -> ensureCapacity(List.of(new OrderItemRequest(POOJA_ID, 1)), pooja))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo(ErrorCode.OFFERING_INACTIVE));
        verifyNoInteractions(orderItemRepository);
    }

    private void ensureCapacity(List<OrderItemRequest> items, Offering offering) {
        dailyLimitService.ensureCapacity(DATE, items, Map.of(offering.getId(), offering));
    }

    private void givenBooked(Long offeringId, long booked) {
        when(orderItemRepository.sumQuantity(offeringId, DATE, OrderStatus.CONFIRMED)).thenReturn(booked);
    }

    private static Offering offering(Long id, String name, Integer dailyLimit) {
        var offering = new Offering(new Temple("Guruvayur"), name, new BigDecimal("100.00"), true, dailyLimit);
        ReflectionTestUtils.setField(offering, "id", id);
        return offering;
    }
}
