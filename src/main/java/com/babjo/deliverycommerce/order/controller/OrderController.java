package com.babjo.deliverycommerce.order.controller;

import com.babjo.deliverycommerce.order.dto.OrderRequestDto;
import com.babjo.deliverycommerce.order.dto.OrderResponseDto;
import com.babjo.deliverycommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // 사용자의 주문 내역 조회
    @GetMapping
    public ResponseEntity<Page<OrderResponseDto>> getMyOrders(
            // TODO : spring security 도입 시 @AuthenticationPrincipal 변경 예정
            @RequestHeader(name = "UserId") Long userId,
            // default size 10
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
            ){
        Page<OrderResponseDto> response = orderService.getUserOders(userId, pageable);
        return ResponseEntity.ok(response);
    }

    // 주문 취소 PATCH /v1/orders/{orderId}/cancel
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            // TODO : spring security 도입 시 @AuthenticationPrincipal 변경 예정
            @RequestHeader(name = "UserId")Long userId,
            @PathVariable UUID orderId,
            @RequestParam String reason
    ){
        orderService.cancelOrder(orderId,userId, reason);
        return ResponseEntity.noContent().build();
    }

    // 주문 접수
    @PatchMapping("/{orderId}/accept")
    public ResponseEntity<OrderResponseDto> acceptOrder(
            @PathVariable UUID orderId,
            @RequestBody OrderRequestDto.AcceptOrder request,
            // TODO : spring security 도입 시 @AuthenticationPrincipal 변경 예정
            @RequestHeader(name = "OwnerId") Long ownerId
    ){
        OrderResponseDto response = orderService.acceptOrder(orderId, ownerId, request);
        return ResponseEntity.ok(response);
    }

}
