package com.babjo.deliverycommerce.user.repository;

import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.user.entity.QUser;
import com.babjo.deliverycommerce.user.entity.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;


/**
 * QueryDSL 기반 사용자 검색 repository
 *
 * QUser: User 엔티티의 QueryDSL 메타모델 (빌드 시 자동 생성)
 * BooleanBuilder: 동적으로 WHERE 조건을 조합하는 빌더
 * OrderSpecifier: ORDER BY 절을 표현하는 객체
 */

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 관리자용 사용자 상세 검색 (페이지)
     *
     * 검색 조건과 정렬 조건 분리
     * Pageable은 전달만 받고, 실제 QueryDSL 변환 책임은 repository에서 해결
     * service 레이어가 QueryDSL 타입(QUser 등)을 알지 못하도록 함
     * 검색 조건 null이면 해당 조건 무시
     *
     * @param username - 아이디 부분 검색 (대소문자 무시)
     * @param nickname - 닉네임 부분 검색 (대소문자 무시)
     * @param role - 역할 필터 (CUSTOMER, OWNER, MANAGER, MASTER)
     * @param includeDeleted - true: 탈퇴 회원 포함 / false: 제외
     * @param pageable - 페이징 및 정렬 정보
     * @return 검색 결과 Page
     *
     */
    public Page<User> searchUsers(
            String username,
            String nickname,
            String role,
            boolean includeDeleted,
            Pageable pageable
    ) {

        QUser user = QUser.user;

        // 검색 조건 생성
        BooleanBuilder condition = buildSearchCondition(
                username,
                nickname,
                role,
                includeDeleted,
                user
        );

        // 실제 데이터 조회 쿼리
        List<User> content = queryFactory
                .selectFrom(user)
                .where(condition) // WHERE조건 > condition 객체값
                .offset(pageable.getOffset()) // offset (현재 페이지 * 사이즈)
                .limit(pageable.getPageSize()) // limit
                .orderBy(getOrderSpecifier(pageable, user)) // getOrderSpecifire 값으로 orderBy 선정
                .fetch(); // 결과를 List로 반환

        /**
         * Page 객체 생성
         *
         * PageableExecutionUtils.getPage():
         * - content 사이즈가 pageSize보다 작으면 count 쿼리를 생략
         * - 마지막 페이지에서 불필요한 count 쿼리를 방지
         */
        return PageableExecutionUtils.getPage(
                content,
                pageable,
                () -> queryFactory
                        .select(user.count())
                        .from(user)
                        .where(condition)
                        .fetchOne()
        );
    }

    /**
     * 검색 조건(BooleanBuilder) 생성 메서드
     * - BooleanBuilder를 사용해 null이 아닌 조건만 WHERE절에 추가
     * - searchUsers() 메서드와 독립적으로 분리함
     * - 검색 조건이 늘어나더라도 구조가 무너지지 않게 하기 위함
     */
    private BooleanBuilder buildSearchCondition(
            String username,
            String nickname,
            String role,
            boolean includeDeleted,
            QUser user
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        // username 부분 검색 (대소문자 무시)
        if (StringUtils.hasText(username)) {
            builder.and(user.username.containsIgnoreCase(username));
        }

        // nickname 부분 검색 (대소문자 무시)
        if (StringUtils.hasText(nickname)) {
            builder.and(user.nickname.containsIgnoreCase(nickname));
        }

        // 역할 필터
        if (StringUtils.hasText(role)) {
            try {
                UserEnumRole userRole = UserEnumRole.valueOf(role);
                builder.and(user.role.eq(userRole));
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_ROLE);
            }
        }

        // soft delete 사용자 제외 여부
        if (!includeDeleted) {
            builder.and(user.deletedAt.isNull());
        }

        return builder;
    }

    /**
     * Pageable → QueryDSL 정렬 조건 변환
     *
     * Spring Data의 Sort와 QueryDSL의 OrderSpecifier는 직접 호환되지 않아
     * 직접 수동 변환 필요
     *
     * @param pageable - 정렬 관련 정보 포함 (sortBy, isAsc)
     * @param user - QUser 메타모델
     * @return QueryDSL 정렬 조건
     * @throws IllegalStateException 허용되지 않은 정렬 필드 (DTO 검증 통과 후 발생 시)
     */
    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable, QUser user) {

        Sort.Order sortOrder = pageable.getSort().stream()
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_SORT_BY));
        //                         ↑ 정렬 조건이 없으면 명시적으로 예외 처리

        Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

        return switch (sortOrder.getProperty()) {
            case "username"  -> new OrderSpecifier<>(direction, user.username);
            case "nickname"  -> new OrderSpecifier<>(direction, user.nickname);
            case "createdAt" -> new OrderSpecifier<>(direction, user.createdAt);
            default -> throw new CustomException(ErrorCode.INVALID_SORT_BY);
        };
    }
}