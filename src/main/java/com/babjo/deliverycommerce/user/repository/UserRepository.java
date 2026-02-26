package com.babjo.deliverycommerce.user.repository;

import com.babjo.deliverycommerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
