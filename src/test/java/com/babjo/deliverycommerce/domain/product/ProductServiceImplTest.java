package com.babjo.deliverycommerce.domain.product;

import com.babjo.deliverycommerce.domain.ai.repository.AiRequestLogRepository;
import com.babjo.deliverycommerce.domain.product.repository.ProductRepository;
import com.babjo.deliverycommerce.domain.product.service.AiDescriptionService;
import com.babjo.deliverycommerce.domain.product.service.ProductServiceImpl;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.store.repository.StoreRepository;
import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.domain.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.domain.product.entity.Product;
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
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AiRequestLogRepository aiRequestLogRepository;

    @Mock
    private AiDescriptionService aiDescriptionService;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID storeId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    // 정상 상품 등록
    @Test
    void createProduct_success() {

        // given
        ProductCreateRequestDto request = new ProductCreateRequestDto(
                "알리오올리오", 12000, "파스타", "맛있는 파스타", false
        );

        UserPrincipal user = mock(UserPrincipal.class);
        when(user.getRole()).thenReturn(UserEnumRole.Authority.OWNER);
        when(user.getUserId()).thenReturn(2L);

        Store store = mock(Store.class);
        when(store.getCreatedBy()).thenReturn(2L);
        when(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).thenReturn(Optional.of(store));

        // when
        ProductResponseDto response = productService.create(storeId, request, user);

        // then
        verify(productRepository, times(1)).save(any(Product.class));
        assertThat(response.getName()).isEqualTo("알리오올리오");
        assertThat(response.getPrice()).isEqualTo(12000);
    }

    // 존재하지 않는 상품 조회
    @Test
    void getProduct_notFound() {
        // given
        when(productRepository.findByProductIdAndStore_StoreId(storeId, productId))
                .thenReturn(Optional.empty());

        UserPrincipal user = mock(UserPrincipal.class);

        // when & then
        assertThatThrownBy(() -> productService.get(storeId, productId, user))
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

        when(productRepository.findByProductIdAndStore_StoreId(storeId, productId))
                .thenReturn(Optional.of(product));

        UserPrincipal user = mock(UserPrincipal.class);
        when(user.getRole()).thenReturn(UserEnumRole.Authority.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> productService.get(storeId, productId, user))
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
        when(productRepository.findByProductIdAndStore_StoreId(storeId, productId))
                .thenReturn(Optional.of(product));

        UserPrincipal user = mock(UserPrincipal.class);

        // when & then
        assertThatThrownBy(() -> productService.get(storeId, productId, user))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_DELETED);
                });
    }

    // 숨김 상품 조회
    @Test
    void OWNER라도_본인의_store가_아니라면_숨김_상품_조회_불가() {

        // given
        Product product = Product.builder()
                .name("페퍼로니 피자")
                .price(15000)
                .productCategory("피자")
                .description("쭉 늘어나는 치즈")
                .useAiDescription(false)
                .build();
        product.hide();

        when(productRepository.findByProductIdAndStore_StoreId(storeId, productId))
                .thenReturn(Optional.of(product));

        UserPrincipal user = mock(UserPrincipal.class);
        when(user.getRole()).thenReturn(UserEnumRole.Authority.OWNER);
        when(user.getUserId()).thenReturn(2L);

        Store store = mock(Store.class);
        when(store.getCreatedBy()).thenReturn(999L);    // 다른 사람이 소유
        when(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).thenReturn(Optional.of(store));

        // when & then
        assertThatThrownBy(() -> productService.get(storeId, productId, user))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    // 숨김 상품은 존재 자체를 숨기기 위해 404 처리
                    assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                });
    }

    @Test
    void 본인_store_OWNER는_숨김_상품_조회_가능() {

        // given
        Product product = Product.builder()
                .name("페퍼로니 피자")
                .price(15000)
                .productCategory("피자")
                .description("쭉 늘어나는 치즈")
                .useAiDescription(false)
                .build();
        product.hide();

        when(productRepository.findByProductIdAndStore_StoreId(storeId, productId))
                .thenReturn(Optional.of(product));

        UserPrincipal user = mock(UserPrincipal.class);
        when(user.getRole()).thenReturn(UserEnumRole.Authority.OWNER);
        when(user.getUserId()).thenReturn(2L);

        Store store = mock(Store.class);
        when(store.getCreatedBy()).thenReturn(2L);    // 다른 사람이 소유
        when(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).thenReturn(Optional.of(store));

        // when
        ProductResponseDto response = productService.get(storeId, productId, user);

        // then
        assertThat(response.getName()).isEqualTo("페퍼로니 피자");
    }

    @Test
    void MANAGER는_소유_무관_숨김_상품_조회_가능() {

        // given
        Product product = Product.builder()
                .name("페퍼로니 피자")
                .price(15000)
                .productCategory("피자")
                .description("쭉 늘어나는 치즈")
                .useAiDescription(false)
                .build();
        product.hide();

        when(productRepository.findByProductIdAndStore_StoreId(storeId, productId))
                .thenReturn(Optional.of(product));

        UserPrincipal user = mock(UserPrincipal.class);
        when(user.getRole()).thenReturn(UserEnumRole.Authority.MANAGER);

        // when
        ProductResponseDto response = productService.get(storeId, productId, user);

        // then
        assertThat(response.getName()).isEqualTo("페퍼로니 피자");

        // MANAGER는 store 조회를 안 해도 되므로 storeRepository 호출이 없어야 자연스러움
        verify(storeRepository, never()).findByStoreIdAndDeletedAtIsNull(any());
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

        when(productRepository.findByProductIdAndStore_StoreIdAndDeletedAtIsNull(productId, storeId))
                .thenReturn(Optional.of(product));
        when(aiDescriptionService.generateProductDescription("트러플 크림 파스타", "트러플 향"))
                .thenReturn("트러플 향 가득한 부드러운 크림 파스타!");

        UserPrincipal user = mock(UserPrincipal.class);
        when(user.getRole()).thenReturn(UserEnumRole.Authority.OWNER);
        when(user.getUserId()).thenReturn(2L);

        Store store = mock(Store.class);
        when(store.getCreatedBy()).thenReturn(2L);
        when(storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)).thenReturn(Optional.of(store));

        // when
        ProductResponseDto result = productService.generateDescription(storeId, productId, "트러플 향", user);

        // then
        assertThat(result.getDescription()).isEqualTo("트러플 향 가득한 부드러운 크림 파스타!");
        assertThat(product.getDescription()).isEqualTo("트러플 향 가득한 부드러운 크림 파스타!");
        assertThat(product.getUseAiDescription()).isTrue();
    }

}