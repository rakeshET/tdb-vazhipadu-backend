package com.tdb.vazhipadu.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public record DailyCollectionReport(
        Long templeId,
        LocalDate date,
        ZoneId timeZone,
        BigDecimal totalAmount,
        List<OfferingCollection> offerings) {
}
