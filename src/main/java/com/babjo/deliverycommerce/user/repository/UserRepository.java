package com.babjo.deliverycommerce.user.repository;

import com.babjo.deliverycommerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 중복검사는 count(*) 보다 exist로 처리
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);
}