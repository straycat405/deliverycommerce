package com.babjo.deliverycommerce.domain.ai.controller;

import com.babjo.deliverycommerce.domain.ai.dto.AiRequestLogResponseDto;
import com.babjo.deliverycommerce.domain.ai.service.AiRequestLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/products/{productId}/ai-logs")
public class AiRequestLogController {

    private final AiRequestLogService aiRequestLogService;

    @PreAuthorize("hasAnyRole('MANAGER','MASTER', 'OWNER')")
    @GetMapping
    public Page<AiRequestLogResponseDto> getLogs(
            @PathVariable UUID productId,
            Pageable pageable
    ) {
        return aiRequestLogService.getLogs(productId, pageable);
    }
}