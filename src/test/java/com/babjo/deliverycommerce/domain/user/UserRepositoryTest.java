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

import java.util.Optional;

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
        @DisplayName("존재하는 아이디면 true를 반환한다")
        void withExistingUsername() {
            // given
            userRepository.save(createUser("testuser", "test@test.com"));

            // when
            boolean result = userRepository.existsByUsernameAll("testuser");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 아이디면 false를 반환한다")
        void withNonExistentUsername() {
            // when
            boolean result = userRepository.existsByUsernameAll("notestuser");

            // then
            assertThat(result).isFalse();
        }

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

    @Nested
    @DisplayName("existsByEmailAll")
    class ExistsByEmailAll {

        @Test
        @DisplayName("존재하는 이메일이면 true를 반환한다")
        void withExistingEmail() {
            // given
            userRepository.save(createUser("testuser", "test@test.com"));

            // when
            boolean result = userRepository.existsByEmailAll("test@test.com");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 email이면 false를 반환한다")
        void withNonExistentEmail() {
            // when
            boolean result = userRepository.existsByEmailAll("noexistent@test.com");

            // then
            assertThat(result).isFalse();
        }

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

    @Nested
    @DisplayName("findByUsername")
    class FindByUsername {

        @Test
        @DisplayName("존재하는 아이디면 그 User를 반환한다")
        void withExistingUsername() {
            // given
            userRepository.save(createUser("testuser", "test@test.com"));

            // when
            Optional<User> result = userRepository.findByUsername("testuser");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
            assertThat(result.get().getEmail()).isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("존재하지 않는 아이디면 Optional.empty()를 반환한다")
        void withNonExistentUsername() {
            // when
            Optional<User> result = userRepository.findByUsername("nonexistent");

            // then
            assertThat(result).isEmpty();
        }
    }
}
