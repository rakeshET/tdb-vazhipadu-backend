package com.tdb.vazhipadu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdb.vazhipadu.dto.response.OfferingCollection;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long TEMPLE_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);
    private static final Instant IST_MIDNIGHT = Instant.parse("2026-10-31T18:30:00Z");
    private static final Instant NEXT_IST_MIDNIGHT = Instant.parse("2026-11-01T18:30:00Z");

    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private TempleService templeService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-11-01T06:00:00Z"), ZoneId.of("Asia/Kolkata"));
        reportService = new ReportService(orderItemRepository, templeService, clock);
    }

    @Test
    void shouldQueryTheIstCalendarDayAndSumOfferingTotals() {
        when(orderItemRepository.collectByOffering(TEMPLE_ID, OrderStatus.CONFIRMED, IST_MIDNIGHT, NEXT_IST_MIDNIGHT))
                .thenReturn(List.of(
                        new OfferingCollection(1L, "Archana", 3L, new BigDecimal("99.99")),
                        new OfferingCollection(2L, "Pushpanjali", 7L, new BigDecimal("210.07"))));

        var report = reportService.dailyCollection(TEMPLE_ID, DATE);

        assertThat(report.totalAmount()).isEqualTo(new BigDecimal("310.06"));
        assertThat(report.timeZone()).isEqualTo(ZoneId.of("Asia/Kolkata"));
        assertThat(report.offerings()).hasSize(2);
        verify(templeService).findTempleOrThrow(TEMPLE_ID);
    }

    @Test
    void shouldReturnZeroWithTwoDecimalsWhenNothingCollected() {
        when(orderItemRepository.collectByOffering(TEMPLE_ID, OrderStatus.CONFIRMED, IST_MIDNIGHT, NEXT_IST_MIDNIGHT))
                .thenReturn(List.of());

        var report = reportService.dailyCollection(TEMPLE_ID, DATE);

        assertThat(report.totalAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(report.offerings()).isEmpty();
    }
}
