package com.babjo.deliverycommerce.domain.order.controller;

import com.babjo.deliverycommerce.domain.order.dto.OrderRequestDto;
import com.babjo.deliverycommerce.domain.order.dto.OrderResponseDto;
import com.babjo.deliverycommerce.domain.order.entity.OrderStatus;
import com.babjo.deliverycommerce.domain.order.service.OrderService;
import com.babjo.deliverycommerce.global.common.dto.CommonResponse;
import com.babjo.deliverycommerce.global.security.CurrentUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserResolver currentUserResolver;


    // 주문 생성 POST /v1/orders
    @PostMapping
    public ResponseEntity<CommonResponse<OrderResponseDto.OrderDetail>> createOrder(
            Authentication authentication,
            @Valid
            @RequestBody OrderRequestDto.CreateOrder request
    ){
        Long userId = currentUserResolver.getUserId(authentication);
        OrderResponseDto.OrderDetail data = orderService.createOrder(userId, request);
        return CommonResponse.created("주문이 성공적으로 생성되었습니다.",data);
    }

    // 주문 상세 조회 GET /v1/orders/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<CommonResponse<OrderResponseDto.OrderDetail>> getOrder(
            @PathVariable UUID orderId
    ){
        OrderResponseDto.OrderDetail data = orderService.getOrderDetails(orderId);
        return CommonResponse.ok(data);
    }

    // 사용자의 주문 내역 조회
    @GetMapping
    public ResponseEntity<CommonResponse<Page<OrderResponseDto.OrderList>>> getMyOrders(
            Authentication authentication,
            // default size 10
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Long userId = currentUserResolver.getUserId(authentication);
        Page<OrderResponseDto.OrderList> data = orderService.getUserOders(userId, pageable);
        return CommonResponse.ok(data);
    }

    // 주문 취소 PATCH /v1/orders/{orderId}/cancel
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<CommonResponse<OrderResponseDto.OrderAction>> cancelOrder(
            Authentication authentication,
            @PathVariable UUID orderId,
            @RequestParam String reason
    ){
        Long userId = currentUserResolver.getUserId(authentication);
        OrderResponseDto.OrderAction data = orderService.cancelOrder(orderId,userId, reason);
        return CommonResponse.ok("주문이 취소 되었습니다.",data);
    }

    // 주문 내역 삭제 ( 숨김 )
    @DeleteMapping("/{orderId}")
    public ResponseEntity<CommonResponse<OrderResponseDto.OrderAction>> deleteOrder(
            Authentication authentication,
            @PathVariable UUID orderId
    ){
        Long userId = currentUserResolver.getUserId(authentication);
        OrderResponseDto.OrderAction data = orderService.softDeleteOrder(orderId, userId);
        return CommonResponse.ok("주문 내역이 삭제되었습니다.", data);
    }

    // 주문 접수
    @PatchMapping("/{orderId}/accept")
    public ResponseEntity<CommonResponse<OrderResponseDto.OrderAction>> acceptOrder(
            @PathVariable UUID orderId,
            @RequestBody OrderRequestDto.AcceptOrder request,
            Authentication authentication
    ){
        Long ownerId = currentUserResolver.getUserId(authentication);
        OrderResponseDto.OrderAction data = orderService.acceptOrder(orderId, ownerId, request);
        return CommonResponse.ok("주문을 접수하였습니다.",data);
    }

    // 조리 시작
    @PatchMapping("/{orderId}/preparing")
    public ResponseEntity<CommonResponse<OrderResponseDto.OrderAction>> startPreparing(
            @PathVariable UUID orderId,
            Authentication authentication
    ){
        Long ownerId = currentUserResolver.getUserId(authentication);
        OrderResponseDto.OrderAction data = orderService.updateOrderStatus(orderId, ownerId, OrderStatus.PREPARING);
        return CommonResponse.ok("조리를 시작합니다", data);
    }

    // 조리 완료 및 픽업 대기
    @PatchMapping("/{orderId}/pickup-ready")
    public ResponseEntity<CommonResponse<OrderResponseDto.OrderAction>> readyPickup(
            @PathVariable UUID orderId,
            Authentication authentication
    ){
        Long ownerId = currentUserResolver.getUserId(authentication);
        OrderResponseDto.OrderAction data = orderService.updateOrderStatus(orderId, ownerId, OrderStatus.PICKUP_READY);
        return CommonResponse.ok("조리가 완료되어 픽업 대기 상태로 변경 되었습니다.",data);
    }

    // 픽업 완료
    @PatchMapping("/{orderId}/pickup")
    public ResponseEntity<CommonResponse<OrderResponseDto.OrderAction>> completePickup(
            @PathVariable UUID orderId,
            Authentication authentication
    ){
        Long ownerId = currentUserResolver.getUserId(authentication);
        OrderResponseDto.OrderAction data = orderService.updateOrderStatus(orderId, ownerId, OrderStatus.PICKED_UP);
        return CommonResponse.ok("픽업이 완료 되었습니다.",data);
    }

}
