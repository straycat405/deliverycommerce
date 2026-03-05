package com.babjo.deliverycommerce.domain.service;

import com.babjo.deliverycommerce.domain.product.service.AiDescriptionService;
import com.babjo.deliverycommerce.domain.product.service.ProductServiceImpl;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.domain.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.domain.product.entity.Product;
import com.babjo.deliverycommerce.domain.product.repository.ProductRespository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRespository productRespository;

    @Mock
    private AiDescriptionService aiDescriptionService;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
    }

    // 정상 상품 등록
    @Test
    void createProduct_success() {

        // given
        ProductCreateRequestDto request = new ProductCreateRequestDto(
                "알리오올리오", 12000, "파스타", "맛있는 파스타", false
        );

        // when
        ProductResponseDto response = productService.create(request);

        // then
        verify(productRespository, times(1)).save(any(Product.class));

        assertThat(response.getName()).isEqualTo("알리오올리오");
        assertThat(response.getPrice()).isEqualTo(12000);
    }

    // 존재하지 않는 상품 조회
    @Test
    void getProduct_notFound() {
        // given
        when(productRespository.findByProductIdAndDeletedAtIsNull(productId)).thenReturn(Optional.empty());

        UserPrincipal user = mock(UserPrincipal.class);

        // when & then
        assertThatThrownBy(() -> productService.get(productId, user))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                });
    }

    // 숨김 상품 CUSTOMER 조회 불가
    @Test
    void hiddenProduct_customerCannotView() {

        // given
        Product product = Product.builder()
                .name("페퍼로니 피자")
                .price(15000)
                .productCategory("피자")
                .description("쭉 늘어나는 치즈")
                .useAiDescription(false)
                .build();

        product.hide();

        when(productRespository.findByProductIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));

        UserPrincipal user = mock(UserPrincipal.class);
        when(user.getRole()).thenReturn("CUSTOMER");

        // when & then
        assertThatThrownBy(() -> productService.get(productId, user))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                });
    }

    // 삭제 상품 조회 불가
    @Test
    void deletedProduct_accessDenied() {

        // given
        Product product = Product.builder()
                .name("페퍼로니 피자")
                .price(15000)
                .productCategory("피자")
                .description("쭉 늘어나는 치즈")
                .useAiDescription(false)
                .build();

        product.delete(1L);

        // 현재 서비스의 getActiveProduct()는 findByProductIdAndDeletedAtIsNull로 조회해서
        // 삭제된 상품은 보통 Optional.empty가 되어야 자연스러움.
        // 하지만 지금 코드에는 deletedAt 체크도 있으니 테스트에서 해당 흐름을 강제로 태우려면 Optional.of(...)로 준다.
        when(productRespository.findByProductIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));

        UserPrincipal user = mock(UserPrincipal.class);

        // when & then
        assertThatThrownBy(() -> productService.get(productId, user))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_DELETED);
                });
    }

    // 상품 AI 설명 업데이트
    @Test
    void generateDescription_success_shouldUpdateDescription() {

        // given
        Product product = Product.builder()
                .name("트러플 크림 파스타")
                .price(23000)
                .productCategory("파스타")
                .description(null)
                .useAiDescription(false)
                .build();

        when(productRespository.findByProductIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(aiDescriptionService.generateProductDescription("트러플 크림 파스타", "트러플 향")).thenReturn("트러플 향 가득한 부드러운 크림 파스타!");

        // when
        ProductResponseDto result = productService.generateDescription(productId, "트러플 향");

        // then
        assertThat(result.getDescription()).isEqualTo("트러플 향 가득한 부드러운 크림 파스타!");
        assertThat(product.getDescription()).isEqualTo("트러플 향 가득한 부드러운 크림 파스타!");
        assertThat(product.getUseAiDescription()).isTrue();
    }

}