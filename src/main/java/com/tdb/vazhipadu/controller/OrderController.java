package com.tdb.vazhipadu.controller;

import com.tdb.vazhipadu.dto.request.CreateOrderRequest;
import com.tdb.vazhipadu.dto.response.OrderResponse;
import com.tdb.vazhipadu.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/temples/{templeId}/orders")
public class OrderController {

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

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long templeId, @PathVariable Long orderId) {
        return orderService.getOrder(templeId, orderId);
    }
}
