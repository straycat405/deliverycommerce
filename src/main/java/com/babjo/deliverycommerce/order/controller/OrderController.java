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


}
