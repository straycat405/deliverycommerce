package com.babjo.deliverycommerce.domain.product.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "google.ai.enabled", havingValue = "true")
public class GeminiConfig {

    @Bean
    public Client geminiClient(@Value("${google.ai.api-key}") String apiKey) {
        return new Client();
    }
}
