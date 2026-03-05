package com.babjo.deliverycommerce.domain.store.dto;

import com.babjo.deliverycommerce.domain.store.entity.Store;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class StoreResponseDto {

    private UUID storeId;
    private Long ownerId;
    private String category;
    private String name;
    private String address;
    private Double averageRating;
    private Integer reviewCount;

    public static StoreResponseDto from(Store store) {
        return StoreResponseDto.builder()
                .storeId(store.getStoreId())
                .ownerId(store.getOwnerId())
                .category(store.getCategory())
                .name(store.getName())
                .address(store.getAddress())
                .averageRating(store.getAverageRating())
                .reviewCount(store.getReviewCount())
                .build();
    }

}
