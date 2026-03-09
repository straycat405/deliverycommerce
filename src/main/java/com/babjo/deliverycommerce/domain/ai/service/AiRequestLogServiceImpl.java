package com.babjo.deliverycommerce.domain.ai.service;

import com.babjo.deliverycommerce.domain.ai.dto.AiRequestLogResponseDto;
import com.babjo.deliverycommerce.domain.ai.repository.AiRequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiRequestLogServiceImpl implements AiRequestLogService {

    private final AiRequestLogRepository aiRequestLogRepository;

    @Override
    public Page<AiRequestLogResponseDto> getLogs(UUID productId, Pageable pageable) {
        return aiRequestLogRepository.findAllByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(AiRequestLogResponseDto::from);
    }
}
