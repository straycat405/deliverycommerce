package com.babjo.deliverycommerce.product.dto;

import com.babjo.deliverycommerce.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductResponseDto {

    private UUID productId;
    private String name;
    private Integer price;
    private String productCategory;
    private String description;
    private Boolean productHide;
    private Boolean useAiDescription;

    public static ProductResponseDto from(Product product) {
        return ProductResponseDto.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .price(product.getPrice())
                .productCategory(product.getProductCategory())
                .description(product.getDescription())
                .productHide(product.getProductHide())
                .useAiDescription(product.getUseAiDescription())
                .build();
    }
}
