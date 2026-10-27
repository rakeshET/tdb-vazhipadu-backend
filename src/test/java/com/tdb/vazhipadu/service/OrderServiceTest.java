package com.tdb.vazhipadu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdb.vazhipadu.dto.request.CreateOrderRequest;
import com.tdb.vazhipadu.dto.request.OrderItemRequest;
import com.tdb.vazhipadu.dto.request.OrderSearchCriteria;
import com.tdb.vazhipadu.dto.response.OrderItemResponse;
import com.tdb.vazhipadu.entity.Nakshatra;
import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.entity.OfferingOrder;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.entity.Temple;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.exception.ErrorCode;
import com.tdb.vazhipadu.repository.OfferingRepository;
import com.tdb.vazhipadu.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long TEMPLE_ID = 1L;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T20:00:00Z"), IST);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OfferingRepository offeringRepository;

    @Mock
    private TempleService templeService;

    @Mock
    private DailyLimitService dailyLimitService;

    private OrderService orderService;
    private Temple temple;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, offeringRepository, templeService, dailyLimitService, CLOCK);
        temple = withId(new Temple("Sabarimala"), TEMPLE_ID);
    }

    @Test
    void shouldComputeTotalOnServerFromOfferingPrices() {
        var pushpanjali = offering(10L, temple, "Pushpanjali", "30.00", true);
        var archana = offering(11L, temple, "Archana", "50.00", true);
        when(templeService.findTempleOrThrow(TEMPLE_ID)).thenReturn(temple);
        when(offeringRepository.findAllById(anyIterable())).thenReturn(List.of(pushpanjali, archana));
        when(orderRepository.save(any(OfferingOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = orderService.placeOrder(
                TEMPLE_ID, request(TODAY, new OrderItemRequest(10L, 2), new OrderItemRequest(11L, 1)));

        assertThat(response.totalAmount()).isEqualByComparingTo("110.00");
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.createdAt()).isEqualTo(CLOCK.instant());
        assertThat(response.items())
                .extracting(OrderItemResponse::offeringName, OrderItemResponse::lineTotal)
                .containsExactly(
                        tuple("Pushpanjali", new BigDecimal("60.00")), tuple("Archana", new BigDecimal("50.00")));
    }

    @Test
    void shouldRejectOrderForInactiveOffering() {
        var archana = offering(11L, temple, "Archana", "50.00", false);
        when(templeService.findTempleOrThrow(TEMPLE_ID)).thenReturn(temple);
        when(offeringRepository.findAllById(anyIterable())).thenReturn(List.of(archana));

        assertThatThrownBy(() -> orderService.placeOrder(TEMPLE_ID, request(TODAY, new OrderItemRequest(11L, 1))))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.OFFERING_INACTIVE));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldRejectOfferingThatBelongsToAnotherTemple() {
        var otherTemple = withId(new Temple("Guruvayur"), 2L);
        var foreign = offering(20L, otherTemple, "Udayasthamana Pooja", "5000.00", true);
        when(templeService.findTempleOrThrow(TEMPLE_ID)).thenReturn(temple);
        when(offeringRepository.findAllById(anyIterable())).thenReturn(List.of(foreign));

        assertThatThrownBy(() -> orderService.placeOrder(TEMPLE_ID, request(TODAY, new OrderItemRequest(20L, 1))))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.UNKNOWN_OFFERING));
    }

    @Test
    void shouldRejectUnknownOffering() {
        when(templeService.findTempleOrThrow(TEMPLE_ID)).thenReturn(temple);
        when(offeringRepository.findAllById(anyIterable())).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.placeOrder(TEMPLE_ID, request(TODAY, new OrderItemRequest(99L, 1))))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.UNKNOWN_OFFERING));
    }

    @Test
    void shouldRejectSameOfferingListedTwice() {
        when(templeService.findTempleOrThrow(TEMPLE_ID)).thenReturn(temple);

        assertThatThrownBy(() -> orderService.placeOrder(
                        TEMPLE_ID, request(TODAY, new OrderItemRequest(10L, 1), new OrderItemRequest(10L, 2))))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void shouldRejectOfferingDateInThePastInIst() {
        when(templeService.findTempleOrThrow(TEMPLE_ID)).thenReturn(temple);

        assertThatThrownBy(() ->
                        orderService.placeOrder(TEMPLE_ID, request(TODAY.minusDays(1), new OrderItemRequest(10L, 1))))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void shouldThrowNotFoundWhenOrderMissing() {
        when(orderRepository.findWithItemsByIdAndTempleId(5L, TEMPLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(TEMPLE_ID, 5L))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void shouldCancelOrderPlacedEarlierTheSameIstDay() {
        var order = orderPlacedAt(Instant.parse("2026-10-26T18:30:00Z"));

        var response = orderService.cancelOrder(TEMPLE_ID, 5L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.cancelledAt()).isEqualTo(CLOCK.instant());
        assertThat(order.isCancelled()).isTrue();
    }

    @Test
    void shouldRejectCancellingAlreadyCancelledOrder() {
        orderPlacedAt(CLOCK.instant()).cancel(CLOCK.instant());

        assertThatThrownBy(() -> orderService.cancelOrder(TEMPLE_ID, 5L))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.ORDER_ALREADY_CANCELLED));
    }

    @Test
    void shouldRejectCancellingOrderPlacedOnEarlierIstDayEvenIfSameUtcDay() {
        var order = orderPlacedAt(Instant.parse("2026-10-26T18:29:59Z"));

        assertThatThrownBy(() -> orderService.cancelOrder(TEMPLE_ID, 5L))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.ORDER_NOT_CANCELLABLE));
        assertThat(order.isCancelled()).isFalse();
    }

    @Test
    void shouldThrowNotFoundWhenCancellingUnknownOrder() {
        when(orderRepository.findForUpdateByIdAndTempleId(5L, TEMPLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(TEMPLE_ID, 5L))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void shouldThrowNotFoundWhenSearchingOrdersOfUnknownTemple() {
        when(templeService.findTempleOrThrow(99L)).thenThrow(ApiException.notFound("Temple", 99L));

        assertThatThrownBy(() -> orderService.searchOrders(99L, new OrderSearchCriteria(null, null, null), 0, 20))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.NOT_FOUND));
        verifyNoInteractions(orderRepository);
    }

    private OfferingOrder orderPlacedAt(Instant placedAt) {
        var order = new OfferingOrder(temple, "Ravi", "9876543210", Nakshatra.ROHINI, TODAY.plusDays(3), placedAt);
        when(orderRepository.findForUpdateByIdAndTempleId(5L, TEMPLE_ID)).thenReturn(Optional.of(order));
        return order;
    }

    private static CreateOrderRequest request(LocalDate date, OrderItemRequest... items) {
        return new CreateOrderRequest("Ravi", "9876543210", Nakshatra.ROHINI, date, List.of(items));
    }

    private static Offering offering(Long id, Temple temple, String name, String price, boolean active) {
        return withId(new Offering(temple, name, new BigDecimal(price), active), id);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
