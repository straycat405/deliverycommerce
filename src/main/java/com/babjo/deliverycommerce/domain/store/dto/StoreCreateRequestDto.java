package com.babjo.deliverycommerce.domain.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class StoreCreateRequestDto {

    @NotBlank
    @Size(max = 50)
    private String category;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String address;
}
