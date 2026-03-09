package com.babjo.deliverycommerce.domain.ai.service;

import com.babjo.deliverycommerce.domain.ai.dto.AiRequestLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AiRequestLogService {
    Page<AiRequestLogResponseDto> getLogs(UUID productId, Pageable pageable);
}
