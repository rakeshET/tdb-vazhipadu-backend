package com.tdb.vazhipadu.dto.response;

import java.math.BigDecimal;

public record OfferingResponse(
        Long id,
        Long templeId,
        String name,
        BigDecimal price,
        boolean active,
        Integer dailyLimit) {
}
