package com.babjo.deliverycommerce.order.controller;

import com.babjo.deliverycommerce.order.dto.OrderRequestDto;
import com.babjo.deliverycommerce.order.dto.OrderResponseDto;
import com.babjo.deliverycommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;

    // 주문 생성 POST /v1/orders
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @RequestHeader(name = "UserId") Long userId,
            @Valid
            @RequestBody OrderRequestDto.CreateOrder request
    ){
        OrderResponseDto response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 주문 상세 조회 GET /v1/orders/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrder(
            @PathVariable UUID orderId
    ){
        OrderResponseDto response = orderService.getOrderDetails(orderId);
        return ResponseEntity.ok(response);
    }

    // 주문 취소 PATCH /v1/orders/{orderId}/cancel
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @RequestHeader(name = "UserId")Long userId,
            @PathVariable UUID orderId,
            @RequestParam String reason
    ){
        orderService.cancelOrder(orderId,userId, reason);
        return ResponseEntity.noContent().build();
    }

}
