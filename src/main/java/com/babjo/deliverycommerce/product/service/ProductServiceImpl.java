package com.babjo.deliverycommerce.product.service;

import com.babjo.deliverycommerce.product.dto.ProductCreateRequestDto;
import com.babjo.deliverycommerce.product.dto.ProductResponseDto;
import com.babjo.deliverycommerce.product.dto.ProductUpdateRequestDto;
import com.babjo.deliverycommerce.product.entity.Product;
import com.babjo.deliverycommerce.product.repository.ProductRespository;
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
    private final AiDescriptionService aiDescriptionService;

    // AI 이후 연결

    // 상품 생성
    @Override
    @Transactional
    public ProductResponseDto create(ProductCreateRequestDto request) {

        String description = request.getDescription();

        // AI 사용 시
        if (Boolean.TRUE.equals(request.getUseAiDescription())) {
            // AI 코드 추가
            description = "AI 생성 설명(임시)";
        }

        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .productCategory(request.getProductCategory())
                .description(description)
                .useAiDescription(request.getUseAiDescription())
                .build();

        productRespository.save(product);

        return ProductResponseDto.from(product);
    }

    // 단건 조회(owner 전용)
    @Override
    public ProductResponseDto get(UUID productId) {

        Product product = productRespository
                .findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        return ProductResponseDto.from(product);
    }

    // 전체 조회(owner 전용)
    @Override
    public List<ProductResponseDto> getAll() {
        return productRespository.findAllByDeletedAtIsNull()
                .stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 단건 조회(customer 전용: 숨김처리 한 상품 표시 X)

    // 전체 조회(customer 전용)

    // 수정
    @Override
    @Transactional
    public ProductResponseDto update(UUID productId, ProductUpdateRequestDto request) {

        Product product = productRespository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        String description = request.getDescription();

        // AI 사용 시
        if (Boolean.TRUE.equals(request.getUseAiDescription())) {
            // AI 코드 추가
            description = "AI 생성 설명(임시)";
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
    public void delete(UUID productId, Long userId) {

        Product product = productRespository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        product.delete(userId);
    }

    @Override
    @Transactional
    public ProductResponseDto geberateDescription(UUID productId, String point) {

        Product product = productRespository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        String aiDescription = aiDescriptionService.generateProductDescription(product.getName(), point);

        product.updateDescription(aiDescription);

        return ProductResponseDto.from(product);
    }

    @Override
    @Transactional
    public void hide(UUID productId) {
        Product product = productRespository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        product.hide();
    }

    @Override
    @Transactional
    public void show(UUID productId) {
        Product product = productRespository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        product.show();
    }
}
