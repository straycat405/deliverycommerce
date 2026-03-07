package com.babjo.deliverycommerce.domain.user;

import com.babjo.deliverycommerce.domain.user.entity.User;
import com.babjo.deliverycommerce.domain.user.repository.UserQueryRepository;
import com.babjo.deliverycommerce.domain.user.repository.UserRepository;
import com.babjo.deliverycommerce.global.common.audit.AuditorAwareImpl;
import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import com.babjo.deliverycommerce.global.jpa.JpaAuditingConfig;
import com.babjo.deliverycommerce.global.jpa.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 사용자 검색용 QueryDSL 테스트
 */

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class, QueryDslConfig.class, UserQueryRepository.class})
public class UserQueryRepositoryTest {

    @Autowired
    private UserQueryRepository userQueryRepository;

    @Autowired
    private UserRepository userRepository;

    // searchUsers()는 sort 없으면 예외 -> 기본 Pageable 삽입 (Pageable을 현재 service에서 만들어서 넘겨주고있음)
    private PageRequest pageRequest(String sortBy) {
        return PageRequest.of(0, 10, Sort.by(sortBy).ascending());
    }

    private User createUser(String username, String nickname, UserEnumRole role) {
        return User.builder()
                .username(username)
                .password("encodedPassword")
                .email(username + "@test.com")
                .nickname(nickname)
                .role(role)
                .build();
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        @DisplayName("검색 조건 없음 - 활성 사용자 전체 반환")
        void withNoCondition() {
            // given
            userRepository.save(createUser("user1", "nickname1", UserEnumRole.CUSTOMER));
            userRepository.save(createUser("user2", "nickname2", UserEnumRole.OWNER));

            // when
            Page<User> result = userQueryRepository.searchUsers(null, null, null, false, pageRequest("username"));

            // then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("username 부분 검색 - 일치하는 사용자만 반환")
        void withUsernameFilter() {
            // given
            userRepository.save(createUser("test001", "nickname1", UserEnumRole.CUSTOMER));
            userRepository.save(createUser("user002", "nickname2", UserEnumRole.CUSTOMER));

            // when
            Page<User> result = userQueryRepository.searchUsers("test", null, null, false, pageRequest("username"));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("test001");
        }

        @Test
        @DisplayName("username 대소문자 무시 - 일치하는 사용자 반환")
        void withUsernameFilterCaseInsensitive() {
            // given
            userRepository.save(createUser("kimtest", "김테스트", UserEnumRole.CUSTOMER));

            // when
            Page<User> result = userQueryRepository.searchUsers("KIM", null, null, false, pageRequest("username"));

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("nickname 부분 검색 - 일치하는 사용자만 반환")
        void withNicknameFilter() {
            // given
            userRepository.save(createUser("user1", "닉네임테스트", UserEnumRole.CUSTOMER));
            userRepository.save(createUser("user2", "다른닉네임", UserEnumRole.CUSTOMER));

            // when
            Page<User> result = userQueryRepository.searchUsers(null, "테스트", null, false, pageRequest("username"));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getNickname()).isEqualTo("닉네임테스트");
        }

        @Test
        @DisplayName("role 필터링 - 해당 role 사용자만 반환")
        void withRoleFilter() {
            // given
            userRepository.save(createUser("owner1", "사장님1", UserEnumRole.OWNER));
            userRepository.save(createUser("customer1", "고객1", UserEnumRole.CUSTOMER));

            // when
            Page<User> result = userQueryRepository.searchUsers(null, null, "OWNER", false, pageRequest("username"));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("owner1");
        }

        @Test
        @DisplayName("유효하지 않은 role - INVALID_ROLE 예외 발생")
        void withInvalidRole() {
            // when & then
            assertThatThrownBy(() ->
                    userQueryRepository.searchUsers(null, null, "INVALID", false, pageRequest("username")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ROLE);
        }

        @Test
        @DisplayName("includeDeleted=false - 탈퇴 사용자 제외 반환")
        void whenIncludeDeletedFalse() {
            // given
            userRepository.save(createUser("activeuser", "활성유저", UserEnumRole.CUSTOMER));
            User deletedUser = createUser("deleteduser", "삭제유저", UserEnumRole.CUSTOMER);
            userRepository.save(deletedUser);
            deletedUser.delete(1L);
            userRepository.save(deletedUser);

            // when
            Page<User> result = userQueryRepository.searchUsers(null, null, null, false, pageRequest("username"));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("activeuser");
        }

        @Test
        @DisplayName("includeDeleted=true - 탈퇴 사용자 포함 반환")
        void whenIncludeDeletedTrue() {
            // given
            userRepository.save(createUser("activeuser", "활성유저", UserEnumRole.CUSTOMER));
            User deletedUser = createUser("deleteduser", "삭제유저", UserEnumRole.CUSTOMER);
            userRepository.save(deletedUser);
            deletedUser.delete(1L);
            userRepository.save(deletedUser);

            // when
            Page<User> result = userQueryRepository.searchUsers(null, null, null, true, pageRequest("username"));

            // then
            assertThat(result.getContent()).hasSize(2);
        }
    }
}