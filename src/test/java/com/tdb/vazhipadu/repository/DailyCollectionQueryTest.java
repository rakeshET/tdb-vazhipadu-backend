package com.tdb.vazhipadu.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.tdb.vazhipadu.dto.response.OfferingCollection;
import com.tdb.vazhipadu.entity.Nakshatra;
import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.entity.OfferingOrder;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.entity.Temple;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class DailyCollectionQueryTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);
    private static final Instant FROM = DATE.atStartOfDay(IST).toInstant();
    private static final Instant TO = DATE.plusDays(1).atStartOfDay(IST).toInstant();

    @Autowired
    private TempleRepository templeRepository;

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Temple temple;
    private Offering archana;
    private Offering pushpanjali;

    @BeforeEach
    void seed() {
        temple = templeRepository.save(new Temple("Sabarimala"));
        archana = offeringRepository.save(new Offering(temple, "Archana", new BigDecimal("33.33"), true));
        pushpanjali = offeringRepository.save(new Offering(temple, "Pushpanjali", new BigDecimal("30.00"), true));
    }

    @Test
    void shouldCountOrderPlacedAtQuarterPastMidnightIstOnThatDate() {
        placeAt("2026-11-01T00:15:00+05:30", archana, 1);
        placeAt("2026-10-31T23:59:59+05:30", archana, 5);
        placeAt("2026-11-02T00:00:00+05:30", archana, 7);

        var lines = orderItemRepository.collectByOffering(temple.getId(), OrderStatus.CONFIRMED, FROM, TO);

        assertThat(lines)
                .extracting(OfferingCollection::offeringName, OfferingCollection::quantity)
                .containsExactly(tuple("Archana", 1L));
    }

    @Test
    void shouldExcludeCancelledOrders() {
        placeAt("2026-11-01T10:00:00+05:30", pushpanjali, 2);
        var cancelled = placeAt("2026-11-01T11:00:00+05:30", pushpanjali, 3);
        cancelled.cancel(Instant.parse("2026-11-01T06:00:00Z"));
        orderRepository.saveAndFlush(cancelled);

        var lines = orderItemRepository.collectByOffering(temple.getId(), OrderStatus.CONFIRMED, FROM, TO);

        assertThat(lines)
                .extracting(OfferingCollection::quantity, OfferingCollection::amount)
                .containsExactly(tuple(2L, new BigDecimal("60.00")));
    }

    @Test
    void shouldAggregateToThePaisaPerOffering() {
        placeAt("2026-11-01T08:00:00+05:30", archana, 1);
        placeAt("2026-11-01T09:00:00+05:30", archana, 2);
        placeAt("2026-11-01T09:30:00+05:30", pushpanjali, 1);

        var lines = orderItemRepository.collectByOffering(temple.getId(), OrderStatus.CONFIRMED, FROM, TO);

        assertThat(lines)
                .extracting(OfferingCollection::offeringName, OfferingCollection::quantity, OfferingCollection::amount)
                .containsExactly(
                        tuple("Archana", 3L, new BigDecimal("99.99")),
                        tuple("Pushpanjali", 1L, new BigDecimal("30.00")));
    }

    @Test
    void shouldExcludeOtherTemples() {
        var otherTemple = templeRepository.save(new Temple("Guruvayur"));
        var otherArchana = offeringRepository.save(new Offering(otherTemple, "Archana", new BigDecimal("40.00"), true));
        var order = new OfferingOrder(
                otherTemple, "Devotee", "9876543210", Nakshatra.ROHINI, DATE, Instant.parse("2026-11-01T04:00:00Z"));
        order.addItem(otherArchana, 1);
        orderRepository.saveAndFlush(order);

        assertThat(orderItemRepository.collectByOffering(temple.getId(), OrderStatus.CONFIRMED, FROM, TO))
                .isEmpty();
    }

    private OfferingOrder placeAt(String placedAt, Offering offering, int quantity) {
        var order = new OfferingOrder(
                temple,
                "Devotee",
                "9876543210",
                Nakshatra.ROHINI,
                DATE,
                OffsetDateTime.parse(placedAt).toInstant());
        order.addItem(offering, quantity);
        return orderRepository.saveAndFlush(order);
    }
}
