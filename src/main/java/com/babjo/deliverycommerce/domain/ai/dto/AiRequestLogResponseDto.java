package com.babjo.deliverycommerce.domain.ai.dto;

import com.babjo.deliverycommerce.domain.ai.entity.AiRequestLog;
import com.babjo.deliverycommerce.domain.ai.entity.AiRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AiRequestLogResponseDto {

    private UUID logId;
    private UUID productId;
    private Long userId;
    private String model;
    private AiRequestStatus status;
    private String prompt;
    private String response;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static AiRequestLogResponseDto from(AiRequestLog log) {
        return AiRequestLogResponseDto.builder()
                .logId(log.getLogId())
                .productId(log.getProductId())
                .userId(log.getUserId())
                .model(log.getModel())
                .status(log.getStatus())
                .prompt(log.getPrompt())
                .response(log.getResponse())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}