package com.tdb.vazhipadu.controller;

import com.tdb.vazhipadu.dto.request.CreateOrderRequest;
import com.tdb.vazhipadu.dto.request.OrderSearchCriteria;
import com.tdb.vazhipadu.dto.response.OrderResponse;
import com.tdb.vazhipadu.dto.response.OrderSummaryResponse;
import com.tdb.vazhipadu.dto.response.PageResponse;
import com.tdb.vazhipadu.entity.OrderStatus;
import com.tdb.vazhipadu.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/temples/{templeId}/orders")
public class OrderController {

    private static final int MAX_PAGE = 1_000_000;

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@PathVariable Long templeId,
                                                    @Valid @RequestBody CreateOrderRequest request) {
        var created = orderService.placeOrder(templeId, request);
        var location = URI.create("/api/temples/%d/orders/%d".formatted(templeId, created.id()));
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> searchOrders(
            @PathVariable Long templeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @Pattern(regexp = "|[6-9]\\d{9}", message = "must be a 10-digit Indian mobile number") String phone,
            @RequestParam(defaultValue = "0") @Min(0) @Max(MAX_PAGE) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orderService.searchOrders(templeId, new OrderSearchCriteria(date, status, phone), page, size);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long templeId, @PathVariable Long orderId) {
        return orderService.getOrder(templeId, orderId);
    }
}
