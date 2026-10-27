package com.tdb.vazhipadu.service;

import com.tdb.vazhipadu.dto.response.DailyCollectionReport;
import com.tdb.vazhipadu.dto.response.OfferingCollection;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final OrderItemRepository orderItemRepository;
    private final TempleService templeService;
    private final Clock clock;

    public ReportService(OrderItemRepository orderItemRepository, TempleService templeService, Clock clock) {
        this.orderItemRepository = orderItemRepository;
        this.templeService = templeService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DailyCollectionReport dailyCollection(Long templeId, LocalDate date) {
        templeService.findTempleOrThrow(templeId);
        var zone = clock.getZone();
        var from = date.atStartOfDay(zone).toInstant();
        var to = date.plusDays(1).atStartOfDay(zone).toInstant();

        var offerings = orderItemRepository.collectByOffering(templeId, OrderStatus.CONFIRMED, from, to);
        var total = offerings.stream()
                .map(OfferingCollection::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        return new DailyCollectionReport(templeId, date, zone, total, offerings);
    }
}
