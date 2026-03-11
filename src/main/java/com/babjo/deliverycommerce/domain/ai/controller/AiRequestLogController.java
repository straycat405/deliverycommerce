package com.babjo.deliverycommerce.domain.ai.controller;

import com.babjo.deliverycommerce.domain.ai.dto.AiRequestLogResponseDto;
import com.babjo.deliverycommerce.domain.ai.service.AiRequestLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/ai-logs")
@Tag(name = "ai-request-log-controller", description = "AI 요청 로그 조회 API")
public class AiRequestLogController {

    private final AiRequestLogService aiRequestLogService;

    @Operation(
            summary = "AI 요청 로그 조회",
            description = """
                    - 특정 상품의 AI 요청 로그를 조회합니다.
                    - 상품 설명 생성 요청 기록을 확인할 수 있습니다.
                    - [권한] OWNER / MANAGER / MASTER
                    - 페이징 조회 지원
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그 조회 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @PreAuthorize("hasAnyRole('MANAGER','MASTER')")
    @GetMapping("/{productId}")
    public Page<AiRequestLogResponseDto> getLogs(
            @PathVariable UUID productId,
            Pageable pageable
    ) {
        return aiRequestLogService.getLogs(productId, pageable);
    }

    @Operation(
            summary = "AI 요청 로그 전체 조회",
            description = """
                - 모든 상품의 AI 요청 로그를 조회합니다.
                - 관리자용 API입니다.
                - [권한] MASTER / MANAGER
                - 페이징 조회 지원
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전체 로그 조회 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    @GetMapping("/all")
    public Page<AiRequestLogResponseDto> getAllLogs(Pageable pageable) {
        return aiRequestLogService.getAllLogs(pageable);
    }
}