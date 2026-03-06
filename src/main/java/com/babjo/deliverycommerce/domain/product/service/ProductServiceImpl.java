package com.babjo.deliverycommerce.domain.product.service;

import com.babjo.deliverycommerce.domain.product.repository.ProductRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.security.UserPrincipal;
import com.babjo.deliverycommerce.domain.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.domain.product.dto.ProductUpdateRequestDto;
import com.babjo.deliverycommerce.domain.product.entity.Product;
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

    private final ProductRepository productRepository;
    private final AiDescriptionService aiDescriptionService;

    // 상품 생성
    @Override
    @Transactional
    public ProductResponseDto create(ProductCreateRequestDto request) {

        String description = request.getDescription();

        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .productCategory(request.getProductCategory())
                .description(description)
                .useAiDescription(request.getUseAiDescription())
                .build();

        productRepository.save(product);

        return ProductResponseDto.from(product);
    }

    // 단건 조회
    @Override
    public ProductResponseDto get(UUID productId, UserPrincipal user) {

        Product product = getActiveProduct(productId);

        // CUSTOMER는 숨김 상품 조회 불가
        if(user.getRole().equals("CUSTOMER") && product.isProductHide()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return ProductResponseDto.from(product);
    }

    // 전체 조회
    @Override
    public List<ProductResponseDto> getAll(UserPrincipal user) {

        String role = user.getRole();

        List<Product> products;

        // CUSTOMER는 숨김 제외
        if (role.equals("CUSTOMER")) {
            products = productRepository.findAllByProductHideFalseAndDeletedAtIsNull();
        } else {    // OWNER 이상은 숨김 포함
            products = productRepository.findAllByDeletedAtIsNull();
        }

        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponseDto> getByCategory(String category, UserPrincipal user) {

        String role = user.getRole();

        List<Product> products;

        if (role.equals("CUSTOMER")) {
            products = productRepository.findAllByProductCategoryAndDeletedAtIsNullAndProductHideFalse(category);
        } else {
            products = productRepository.findAllByProductCategoryAndDeletedAtIsNull(category);
        }

        return products.stream()
                .map(ProductResponseDto::from)
                .toList();
    }

    // 수정 >> Store과 연결 후 본인 store만 수정할 수 있게 변경
    @Override
    @Transactional
    public ProductResponseDto update(UUID productId, ProductUpdateRequestDto request) {

        Product product = getActiveProduct(productId);

        String description = request.getDescription();

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
    public void delete(UUID productId, Long userId) {

        Product product = getActiveProduct(productId);

        product.delete(userId);
    }

    @Override
    @Transactional
    public ProductResponseDto generateDescription(UUID productId, String point) {

        Product product = getActiveProduct(productId);

        String aiDescription = aiDescriptionService.generateProductDescription(product.getName(), point);

        product.updateDescription(aiDescription);

        return ProductResponseDto.from(product);
    }

    @Override
    @Transactional
    public void hide(UUID productId) {
        Product product = getActiveProduct(productId);

        product.hide();
    }

    @Override
    @Transactional
    public void show(UUID productId) {
        Product product = getActiveProduct(productId);

        product.show();
    }

    private Product getActiveProduct(UUID productId) {

        Product product = productRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if(product.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.PRODUCT_DELETED);
        }

        return product;
    }
}
