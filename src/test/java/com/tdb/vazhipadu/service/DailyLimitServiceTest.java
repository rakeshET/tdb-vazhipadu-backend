package com.tdb.vazhipadu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdb.vazhipadu.dto.request.OrderItemRequest;
import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.entity.Temple;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.exception.ErrorCode;
import com.tdb.vazhipadu.repository.OfferingRepository;
import com.tdb.vazhipadu.repository.OrderItemRepository;
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
    private OfferingRepository offeringRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @InjectMocks
    private DailyLimitService dailyLimitService;

    private final Offering pooja = offering(POOJA_ID, "Udayasthamana Pooja", 5);

    @Test
    void shouldRejectOrderThatWouldExceedDailyLimit() {
        givenBooked(4);

        assertThatThrownBy(() -> dailyLimitService.ensureCapacity(DATE,
                List.of(new OrderItemRequest(POOJA_ID, 2)), Map.of(POOJA_ID, pooja)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo(ErrorCode.DAILY_LIMIT_EXCEEDED);
                    assertThat(ex.getMessage())
                            .isEqualTo("'Udayasthamana Pooja' has 1 of 5 slots left on 2026-11-01");
                });
    }

    @Test
    void shouldAllowOrderThatExactlyFillsDailyLimit() {
        givenBooked(4);

        assertThatCode(() -> dailyLimitService.ensureCapacity(DATE,
                List.of(new OrderItemRequest(POOJA_ID, 1)), Map.of(POOJA_ID, pooja)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotLockOrQueryOfferingsWithoutDailyLimit() {
        var pushpanjali = offering(1L, "Pushpanjali", null);

        dailyLimitService.ensureCapacity(DATE, List.of(new OrderItemRequest(1L, 50)), Map.of(1L, pushpanjali));

        verifyNoInteractions(offeringRepository, orderItemRepository);
    }

    private void givenBooked(long booked) {
        when(offeringRepository.lockAllByIdInOrderById(anyCollection())).thenReturn(List.of(pooja));
        when(orderItemRepository.sumQuantity(POOJA_ID, DATE, OrderStatus.CONFIRMED)).thenReturn(booked);
    }

    private static Offering offering(Long id, String name, Integer dailyLimit) {
        var offering = new Offering(new Temple("Guruvayur"), name, new BigDecimal("100.00"), true, dailyLimit);
        ReflectionTestUtils.setField(offering, "id", id);
        return offering;
    }
}
