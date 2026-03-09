package com.babjo.deliverycommerce.domain.product.service;

public interface AiDescriptionService {
    String generateProductDescription(String AI_MODEL, String prompt);
}
