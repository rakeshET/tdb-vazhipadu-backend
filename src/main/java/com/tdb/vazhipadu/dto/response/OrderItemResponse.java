package com.tdb.vazhipadu.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long offeringId,
        String offeringName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
