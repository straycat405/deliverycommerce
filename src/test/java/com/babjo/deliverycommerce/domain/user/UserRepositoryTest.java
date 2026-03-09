package com.babjo.deliverycommerce.domain.user;

import com.babjo.deliverycommerce.domain.user.entity.User;
import com.babjo.deliverycommerce.domain.user.repository.UserRepository;
import com.babjo.deliverycommerce.global.common.audit.AuditorAwareImpl;
import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.global.jpa.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// createdAt(nullable=false)조건 필요해서 추가
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createUser(String username, String email) {
        return User.builder()
                .username(username)
                .password("encodedPassword")
                .email(email)
                .nickname("Tester")
                .role(UserEnumRole.CUSTOMER)
                .build();
    }

    @Nested
    @DisplayName("existsByUsernameAll")
    class ExistsByUsernameAll {

        @Test
        @DisplayName("탈퇴한 사용자의 아이디여도 true를 반환한다")
        void withDeletedUserUsername() {
            // given
            User user = createUser("testuser", "test@test.com");
            userRepository.save(user);
            user.delete(1L); // soft delete 처리
            userRepository.save(user);

            // when
            boolean result = userRepository.existsByUsernameAll("testuser");

            // then
            assertThat(result).isTrue();
        }
    }

    // '탈퇴한 사용자도 포함하는가'에 대한 커스텀 쿼리를 검증하는 것이 핵심이므로, 프레임워크가 검증한 케이스는 제외합니다.

    @Nested
    @DisplayName("existsByEmailAll")
    class ExistsByEmailAll {

        @Test
        @DisplayName("탈퇴한 사용자의 이메일이어도 true를 반환한다")
        void withDeletedUserEmail() {
            // given
            User user = createUser("testuser", "test@test.com");
            userRepository.save(user);
            user.delete(1L);
            userRepository.save(user);

            // when
            boolean result = userRepository.existsByEmailAll("test@test.com");

            // then
            assertThat(result).isTrue();
        }
    }
}
