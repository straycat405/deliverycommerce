package com.babjo.deliverycommerce.domain.store.dto;

import com.babjo.deliverycommerce.domain.store.entity.Store;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class StoreListResponseDto {

    private UUID storeId;
    private String name;
    private String category;
    private Double averageRating;
    private Integer reviewCount;

    public static StoreListResponseDto from(Store store) {
        return new StoreListResponseDto(
                store.getStoreId(),
                store.getName(),
                store.getCategory(),
                store.getAverageRating(),
                store.getReviewCount()

        );
    }

}
