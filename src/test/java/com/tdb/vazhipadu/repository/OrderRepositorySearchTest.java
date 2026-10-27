package com.tdb.vazhipadu.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdb.vazhipadu.dto.request.OrderSearchCriteria;
import com.tdb.vazhipadu.entity.Nakshatra;
import com.tdb.vazhipadu.entity.OfferingOrder;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.entity.Temple;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class OrderRepositorySearchTest {

    private static final LocalDate DAY_ONE = LocalDate.of(2026, 11, 1);
    private static final LocalDate DAY_TWO = LocalDate.of(2026, 11, 2);
    private static final String RAVI = "9876543210";
    private static final String MEERA = "9123456789";

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TempleRepository templeRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Temple temple;

    @BeforeEach
    void seed() {
        temple = templeRepository.save(new Temple("Sabarimala"));
        var otherTemple = templeRepository.save(new Temple("Guruvayur"));
        IntStream.range(0, 25).forEach(i -> orderRepository.save(
                order(temple, i % 2 == 0 ? RAVI : MEERA, i < 15 ? DAY_ONE : DAY_TWO, i % 5 == 0)));
        orderRepository.save(order(otherTemple, RAVI, DAY_ONE, false));
    }

    @Test
    void shouldReturnOnlyOrdersOfThatPhoneWithinTemple() {
        var result = orderRepository.findAll(
                OrderSpecifications.matching(temple.getId(), new OrderSearchCriteria(null, null, RAVI)),
                PageRequest.of(0, 100, Sort.by("id")));

        assertThat(result.getTotalElements()).isEqualTo(13);
        assertThat(result.getContent())
                .allSatisfy(order -> {
                    assertThat(order.getPhone()).isEqualTo(RAVI);
                    assertThat(order.getTemple().getId()).isEqualTo(temple.getId());
                });
    }

    @Test
    void shouldReturnContiguousPagesWithoutRepeatsOrGaps() {
        var spec = OrderSpecifications.matching(temple.getId(), new OrderSearchCriteria(null, null, null));
        var all = orderRepository.findAll(spec, PageRequest.of(0, 100, Sort.by("id"))).getContent();

        var first = orderRepository.findAll(spec, PageRequest.of(0, 10, Sort.by("id"))).getContent();
        var second = orderRepository.findAll(spec, PageRequest.of(1, 10, Sort.by("id"))).getContent();

        assertThat(first).hasSize(10);
        assertThat(second).hasSize(10).doesNotContainAnyElementsOf(first);
        assertThat(second.get(0).getId()).isGreaterThan(first.get(9).getId());
        assertThat(first).containsExactlyElementsOf(all.subList(0, 10));
        assertThat(second).containsExactlyElementsOf(all.subList(10, 20));
    }

    @Test
    void shouldCombineDateAndStatusFilters() {
        var result = orderRepository.findAll(
                OrderSpecifications.matching(temple.getId(),
                        new OrderSearchCriteria(DAY_ONE, OrderStatus.CANCELLED, null)),
                PageRequest.of(0, 100, Sort.by("id")));

        assertThat(result.getContent()).hasSize(3)
                .allSatisfy(order -> {
                    assertThat(order.getOfferingDate()).isEqualTo(DAY_ONE);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                });
    }

    @Test
    void shouldUseIndexesForCommonSearches() {
        assertThat(plan("SELECT id FROM orders WHERE temple_id = 1 AND phone = '9876543210' ORDER BY id"))
                .containsIgnoringCase("IDX_ORDERS_TEMPLE_PHONE");
        assertThat(plan("SELECT id FROM orders WHERE temple_id = 1 AND offering_date = DATE '2026-11-01' ORDER BY id"))
                .containsIgnoringCase("IDX_ORDERS_TEMPLE_DATE");
        assertThat(plan("SELECT id FROM orders WHERE temple_id = 1 AND status = 'CONFIRMED' ORDER BY id"))
                .containsIgnoringCase("IDX_ORDERS_TEMPLE_STATUS");
    }

    private String plan(String sql) {
        return jdbcTemplate.queryForObject("EXPLAIN " + sql, String.class);
    }

    private static OfferingOrder order(Temple temple, String phone, LocalDate date, boolean cancelled) {
        var order = new OfferingOrder(temple, "Devotee", phone, Nakshatra.ROHINI, date, Instant.now());
        if (cancelled) {
            ReflectionTestUtils.setField(order, "status", OrderStatus.CANCELLED);
        }
        return order;
    }
}
