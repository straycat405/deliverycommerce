package com.babjo.deliverycommerce.domain.store.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;


@Getter
public class StoreUpdateRequestDto {

    @Size(max = 50)
    private String category;

    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String address;
}
