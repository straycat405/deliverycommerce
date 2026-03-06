package com.babjo.deliverycommerce.domain.product.service;

import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.store.repository.StoreRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.domain.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductUpdateRequestDto;
import com.babjo.deliverycommerce.domain.product.entity.Product;
import com.babjo.deliverycommerce.domain.product.repository.ProductRespository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRespository productRespository;
    private final StoreRepository storeRepository;
    private final AiDescriptionService aiDescriptionService;

    /* =========================================================
       관리 API (OWNER/MANAGER/MASTER)
       - storeId 필수
       - OWNER는 본인 store 소유일 때만 가능
       - MANAGER/MASTER는 소유 무관
     ========================================================= */

    // 상품 생성
    @Override
    @Transactional
    public ProductResponseDto create(UUID storeId, ProductCreateRequestDto request, UserPrincipal user) {

        validateStoreAccess(storeId, user);

        Store store = storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        String description = request.getDescription();
        if(description == null) {
            description = "";
        }

        Product product = Product.builder()
                .store(store)
                .name(request.getName())
                .price(request.getPrice())
                .productCategory(request.getProductCategory())
                .description(description)
                .useAiDescription(request.getUseAiDescription())
                .build();

        productRespository.save(product);

        return ProductResponseDto.from(product);
    }

    // 수정
    @Override
    @Transactional
    public ProductResponseDto update(UUID storeId, UUID productId, ProductUpdateRequestDto request, UserPrincipal user) {

        validateStoreAccess(storeId, user);

        Product product = getActiveProduct(storeId, productId);

        String description = request.getDescription();
        if(description == null) {
            description = "";
        }

        product.update(
                request.getName(),
                request.getPrice(),
                description,
                request.getProductCategory()
        );

        return ProductResponseDto.from(product);
    }

    // 삭제
    @Override
    @Transactional
    public void delete(UUID storeId, UUID productId, UserPrincipal user) {

        validateStoreAccess(storeId, user);

        Product product = getActiveProduct(storeId, productId);
        product.delete(user.getUserId());
    }

    @Override
    @Transactional
    public void hide(UUID storeId, UUID productId, UserPrincipal user) {

        validateStoreAccess(storeId, user);

        Product product = getActiveProduct(storeId, productId);
        product.hide();
    }

    @Override
    @Transactional
    public void show(UUID storeId, UUID productId, UserPrincipal user) {

        validateStoreAccess(storeId, user);

        Product product = getActiveProduct(storeId, productId);
        product.show();
    }

    @Override
    @Transactional
    public ProductResponseDto generateDescription(UUID storeId, UUID productId, String point, UserPrincipal user) {

        validateStoreAccess(storeId, user);

        Product product = getActiveProduct(storeId, productId);

        String aiDescription = aiDescriptionService.generateProductDescription(product.getName(), point);
        product.updateDescription(aiDescription);

        return ProductResponseDto.from(product);
    }

    /* =========================================================
       조회 API (permitAll)
       - storeId 필수 (store 범위로 조회)
       - 숨김 상품은:
           - 본인 store의 OWNER / MANAGER / MASTER 만 조회 가능
           - CUSTOMER 또는 타 store OWNER는 조회 불가(404 처리)
     ========================================================= */

    // 단건 조회
    @Override
    @Transactional
    public ProductResponseDto get(UUID storeId, UUID productId, UserPrincipal user) {

        Product product = getActiveProduct(storeId, productId);

        // 숨김 상품 접근 제어
        if(product.isProductHide() && !canViewHiddenProduct(storeId, user)) {
            // 숨김 상품은 존재 자체를 숨기기 위해 404로 처리
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return ProductResponseDto.from(product);
    }

    // 전체 조회
    @Override
    @Transactional
    public List<ProductResponseDto> getAll(UUID storeId, String categoryOrNull, UserPrincipal user) {

        Boolean canViewHidden = canViewHiddenProduct(storeId, user);

        List<Product> products;

        if (categoryOrNull != null && !categoryOrNull.isBlank()) {
            // 카테고리로 조회

            if (canViewHidden) {
                products = productRespository
                        .findAllByStore_StoreIdAndProductCategoryAndDeletedAtIsNull(storeId, categoryOrNull);
            } else {
                products = productRespository
                        .findAllByStore_StoreIdAndProductCategoryAndDeletedAtIsNullAndProductHideFalse(storeId, categoryOrNull);
            }

        } else {
            // 전체 조회
            if (canViewHidden) {
                products = productRespository
                        .findAllByStore_StoreIdAndDeletedAtIsNull(storeId);
            } else {
                products = productRespository
                        .findAllByStore_StoreIdAndProductHideFalseAndDeletedAtIsNull(storeId);
            }
        }

        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    /* =========================
       공통 유틸/검증 메서드
     ========================= */

    private Product getActiveProduct(UUID storeId, UUID productId) {

        Product product = productRespository.findByProductIdAndStore_StoreIdAndDeletedAtIsNull(storeId, productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if(product.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.PRODUCT_DELETED);
        }

        return product;
    }

    private Store getActiveStore(UUID storeId) {
        return storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
    }

    // 관리 API 권한 검증
    // MASTER, MANAGER: OK
    // OWNER: store 소유자(createBy)와 로그인 userId 일치해야 OK
    private void validateStoreAccess(UUID storeId, UserPrincipal user) {

        if(user == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        String role= user.getRole();

        if(role.equals("MANAGER") || role.equals("MASTER")) {
            return;
        }

        if (role.equals("OWNER")) {
            Store store = getActiveStore(storeId);
            Long ownerUserId = store.getCreatedBy();

            if (ownerUserId == null || !ownerUserId.equals(user.getUserId())) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            return;
        }

        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    // 숨김 상품 조회 가능 여부
    // MASTER, MANAGER: OK
    // OWNER: store 소유자(createBy)와 로그인 userId 일치해야 OK
    // CUSTOMER/비로그인: 불가
    private Boolean canViewHiddenProduct(UUID storeId, UserPrincipal user) {

        if (user == null) {
            return false;
        }

        String role = user.getRole();

        if(role.equals("MANAGER") || role.equals("MASTER")) {
            return true;
        }

        if(role.equals("OWNER")) {
            Store store = getActiveStore(storeId);
            Long ownerId = store.getCreatedBy();
            return ownerId != null && ownerId.equals(user.getUserId());
        }

        return false;
    }
}
