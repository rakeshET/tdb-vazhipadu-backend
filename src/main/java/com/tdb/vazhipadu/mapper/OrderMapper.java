package com.tdb.vazhipadu.mapper;

import com.tdb.vazhipadu.dto.response.OrderItemResponse;
import com.tdb.vazhipadu.dto.response.OrderResponse;
import com.tdb.vazhipadu.dto.response.OrderSummaryResponse;
import com.tdb.vazhipadu.entity.OfferingOrder;
import com.tdb.vazhipadu.entity.OrderItem;

public final class OrderMapper {

    private OrderMapper() {}

    public static OrderResponse toResponse(OfferingOrder order) {
        var items = order.getItems().stream().map(OrderMapper::toResponse).toList();
        return new OrderResponse(
                order.getId(),
                order.getTemple().getId(),
                order.getDevoteeName(),
                order.getPhone(),
                order.getNakshatra(),
                order.getOfferingDate(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getCancelledAt(),
                items);
    }

    public static OrderSummaryResponse toSummary(OfferingOrder order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getDevoteeName(),
                order.getPhone(),
                order.getNakshatra(),
                order.getOfferingDate(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }

    public static OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getOffering().getId(),
                item.getOfferingName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
