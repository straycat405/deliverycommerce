package com.babjo.deliverycommerce.domain.order;

import com.babjo.deliverycommerce.domain.order.dto.OrderRequestDto;
import com.babjo.deliverycommerce.domain.order.dto.OrderResponseDto;
import com.babjo.deliverycommerce.domain.order.entity.Order;
import com.babjo.deliverycommerce.domain.order.entity.OrderItem;
import com.babjo.deliverycommerce.domain.order.entity.OrderStatus;
import com.babjo.deliverycommerce.domain.order.repository.OrderRepository;
import com.babjo.deliverycommerce.domain.order.service.OrderService;
import com.babjo.deliverycommerce.domain.product.entity.Product;
import com.babjo.deliverycommerce.domain.product.repository.ProductRepository;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.store.repository.StoreRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith({MockitoExtension.class})
public class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Test
    @DisplayName("주문 생성 성공 테스트")
    void createOrder_success() {
        Long userId = 1L;
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Integer price = 20000;

        OrderRequestDto.OrderItemRequest itemDto = OrderRequestDto.OrderItemRequest.builder()
                .productId(productId)
                .productName("치킨")
                .orderPrice(price)
                .orderCount(1)
                .build();

        OrderRequestDto.CreateOrder request = OrderRequestDto.CreateOrder.builder()
                .storeId(storeId)
                .address("인천시")
                .message("없습니다.")
                .orderItems(List.of(itemDto))
                .build();

        Product mockProduct = mock(Product.class);
        given(mockProduct.getPrice()).willReturn(price);
        given(productRepository.findById(productId)).willReturn(Optional.of(mockProduct));

        Order order = Order.createOrder(userId, storeId, "인천시", "없습니다.",
                List.of(OrderItem.createOrderItem(productId, "치킨", price, 1)));

        given(orderRepository.save(any(Order.class))).willReturn(order);

        OrderResponseDto.OrderDetail result = orderService.createOrder(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalPrice()).isEqualTo(price);

        verify(productRepository).findById(productId);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 상품 가격 불일치")
    void createOrder_fail_priceMismatch() {
        // given
        Long userId = 1L;
        UUID productId = UUID.randomUUID();

        OrderRequestDto.OrderItemRequest itemDto = OrderRequestDto.OrderItemRequest.builder()
                .productId(productId)
                .orderPrice(100) // 사용자가 100원으로 조작해서 보냄
                .build();

        OrderRequestDto.CreateOrder request = OrderRequestDto.CreateOrder.builder()
                .orderItems(List.of(itemDto))
                .build();

        Product mockProduct = mock(Product.class);
        given(mockProduct.getPrice()).willReturn(20000); // 실제 가격은 20000원
        given(productRepository.findById(productId)).willReturn(Optional.of(mockProduct));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("상품 가격 정보가 일치하지 않습니다."); // ErrorCode 메시지 확인
    }

    @Test
    @DisplayName("주문 취소 실패 - 본인 주문이 아닐 때")
    void cancelOrder_fail_notOwner() {

        UUID orderId = UUID.randomUUID();
        Long orderUserId = 1L;
        Long hackerId = 999L;

        Order order = mock(Order.class);
        given(order.getUserId()).willReturn(orderUserId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));


        assertThatThrownBy(() -> orderService.cancelOrder(orderId, hackerId, "단순변심"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("본인의 주문에 대해서만 처리 가능합니다.");
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    void getOrderDetails_success() {
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        given(order.getOrderId()).willReturn(orderId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        OrderResponseDto.OrderDetail result = orderService.getOrderDetails(orderId);

        assertThat(result).isNotNull();
        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("주문 접수 성공")
    void acceptOrder_success() {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 100L;

        Order order = spy(Order.createOrder(1L, storeId, "주소", "메시지", List.of()));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        Store mockStore = mock(Store.class);
        given(mockStore.getOwnerId()).willReturn(ownerId);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(mockStore));

        OrderRequestDto.AcceptOrder request = new OrderRequestDto.AcceptOrder(30);

        OrderResponseDto.OrderAction result = orderService.acceptOrder(orderId, ownerId, request.getCookingMinutes());

        assertThat(result.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(order.getCookingMinutes()).isEqualTo(30);
        assertThat(order.getAcceptedBy()).isEqualTo(ownerId);

        verify(storeRepository).findByStoreIdAndDeletedAtIsNull(storeId);
    }

    @Test
    @DisplayName("주문 접수 실패 - 가게 소유주가 아닐 때")
    void acceptOrder_fail_notStoreOwner() {

        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long realOwnerId = 100L;
        Long fakeOwnerId = 999L;

        Order order = mock(Order.class);
        given(order.getStoreId()).willReturn(storeId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        Store mockStore = mock(Store.class);
        given(mockStore.getOwnerId()).willReturn(realOwnerId);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId))
                .willReturn(Optional.of(mockStore));

        assertThatThrownBy(() -> orderService.acceptOrder(orderId, fakeOwnerId, 30))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("해당 가게에 대한 권한이 없습니다.");
    }

    @Test
    @DisplayName("주문 거절 성공 - 사장님이 거절 사유와 함께 거절")
    void rejectOrder_success() {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 100L;
        String reason = "재료 소진으로 인한 주문 불가";

        Order order = spy(Order.createOrder(1L, storeId, "인천", "없음", List.of()));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        Store mockStore = mock(Store.class);
        given(mockStore.getOwnerId()).willReturn(ownerId);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(mockStore));

        orderService.rejectOrder(orderId, ownerId, reason);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getCancelReason()).isEqualTo(reason);
        assertThat(order.getCanceledBy()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("주문 상태 변경 성공 - 조리 시작")
    void updateOrderStatus_preparing() {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Long ownerId = 100L;

        Order order = spy(Order.createOrder(1L, storeId, "주소", "메시지", List.of()));
        order.accept(ownerId, 30); // 접수된 상태여야 조리 시작 가능

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        Store mockStore = mock(Store.class);
        given(mockStore.getOwnerId()).willReturn(ownerId);
        given(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).willReturn(Optional.of(mockStore));

        orderService.updateOrderStatus(orderId, ownerId, OrderStatus.PREPARING);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(order.getPreparingStartedBy()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("주문 내역 삭제 성공")
    void softDeleteOrder_success() {
        UUID orderId = UUID.randomUUID();
        Long userId = 1L;

        Order order = spy(Order.createOrder(userId, UUID.randomUUID(), "주소", "메시지", List.of()));

        // 삭제 혹은 완료 상태가 되어야 하므로 삭제 가능한 상태( 취소 상태 )로 강제 변경
        order.cancel(userId, "단순 변심");

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        orderService.softDeleteOrder(orderId, userId);

        assertThat(order.getDeletedAt()).isNotNull(); // 삭제 시간이 기록되었는지 확인
        assertThat(order.getDeletedBy()).isEqualTo(userId);
    }
}
