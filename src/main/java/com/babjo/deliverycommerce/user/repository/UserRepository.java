package com.babjo.deliverycommerce.user.repository;

import com.babjo.deliverycommerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 기존 - @Where 적용됨 (활성 사용자만)
    boolean existsByUsername(String username);

    // 추가 - 삭제된 사용자 포함 체크 (Native Query)
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM p_user WHERE username = :username",
            nativeQuery = true)
    boolean existsByUsernameAll(@Param("username") String username);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM p_user WHERE email = :email",
            nativeQuery = true)
    boolean existsByEmailAll(@Param("email") String email);

    Optional<User> findByUsername(String username);
}