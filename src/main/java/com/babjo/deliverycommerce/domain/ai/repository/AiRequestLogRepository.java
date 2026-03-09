package com.babjo.deliverycommerce.domain.ai.repository;

import com.babjo.deliverycommerce.domain.ai.entity.AiRequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, Long> {

    Page<AiRequestLog> findAllByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);
}
