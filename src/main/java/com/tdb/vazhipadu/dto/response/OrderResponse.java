package com.tdb.vazhipadu.dto.response;

import com.tdb.vazhipadu.entity.Nakshatra;
import com.tdb.vazhipadu.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OrderResponse(
        Long id,
        Long templeId,
        String devoteeName,
        String phone,
        Nakshatra nakshatra,
        LocalDate offeringDate,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderItemResponse> items) {
}
