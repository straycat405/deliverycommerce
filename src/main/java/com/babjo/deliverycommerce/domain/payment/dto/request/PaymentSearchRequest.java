package com.babjo.deliverycommerce.domain.payment.dto.request;

import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.UUID;

/**
 * 결제 검색 요청 DTO
 * GET /v1/payments
 * - size: 10, 30, 50만 허용 (기본값 10)
 * - page: 0-based (기본값 0)
 * - sortBy: 정렬 기준 (createdAt | amount) - 기본값: createdAt
 * - sortDir: 정렬 방향 (desc | asc) - 기본값: desc
 */
@Getter
@Setter
@NoArgsConstructor
public class PaymentSearchRequest {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "amount");

    private UUID paymentId;
    private UUID orderId;
    private PaymentStatus paymentStatus;

    @Min(0)
    private int page = 0;

    private int size = 10;

    private String sortBy = "createdAt";

    private String sortDir = "desc";

    @AssertTrue(message = "페이지 크기는 10, 30, 50만 가능합니다.")
    public boolean isValidSize() {
        return size == 10 || size == 30 || size == 50;
    }

    @AssertTrue(message = "정렬 기준은 createdAt, amount 중 하나여야 합니다.")
    public boolean isValidSortBy() {
        return sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy);
    }

    @AssertTrue(message = "정렬 방향은 asc 또는 desc 여야 합니다.")
    public boolean isValidSortDir() {
        return "asc".equalsIgnoreCase(sortDir) || "desc".equalsIgnoreCase(sortDir);
    }

    public Pageable toPageable() {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
