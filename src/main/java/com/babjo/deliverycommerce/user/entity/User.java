package com.babjo.deliverycommerce.user.entity;

import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import com.babjo.deliverycommerce.global.common.enums.UserEnumRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본생성자
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name="p_user")
public class User extends BaseEntity {


    /**
     * CREATE TABLE "p_user" (
     * 	"user_id"	BIGINT	DEFAULT AUTO_INCREMENT	NOT NULL,
     * 	"username"	VARCHAR(20)		NOT NULL,
     * 	"password"	VARCHAR(255)		NOT NULL,
     * 	"email"	VARCHAR(255)		NOT NULL,
     * 	"nickname"	VARCHAR(50)		NOT NULL,
     * 	"role"	VARCHAR(20)		NOT NULL,
     * 	"created_at"	TIMESTAMP	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
     * 	"created_by"	BIGINT		NULL,
     * 	"updated_at"	TIMESTAMP		NULL,
     * 	"updated_by"	BIGINT		NULL,
     * 	"deleted_at"	TIMESTAMP		NULL,
     * 	"deleted_by"	BIGINT		NULL
     * );
     *
     * COMMENT ON COLUMN "p_user"."user_id" IS 'PK / 유저 고유 식별자 (시리얼넘버)';
     *
     * COMMENT ON COLUMN "p_user"."username" IS 'UNIQUE / 로그인 아이디';
     *
     * COMMENT ON COLUMN "p_user"."password" IS '사용자 비밀번호, BCrypt';
     *
     * COMMENT ON COLUMN "p_user"."email" IS 'UNIQUE / 사용자 이메일';
     *
     * COMMENT ON COLUMN "p_user"."nickname" IS '사용자 닉네임';
     *
     * COMMENT ON COLUMN "p_user"."role" IS '(ENUM) CUSTOMER,OWNER,MANAGER,MASTER';
     *
     * COMMENT ON COLUMN "p_user"."created_at" IS '레코드 생성 시간';
     *
     * COMMENT ON COLUMN "p_user"."created_by" IS 'user_id / 레코드 생성자';
     *
     * COMMENT ON COLUMN "p_user"."updated_at" IS '레코드 수정 시간';
     *
     * COMMENT ON COLUMN "p_user"."updated_by" IS 'user_id / 레코드 수정자';
     *
     * COMMENT ON COLUMN "p_user"."deleted_at" IS '레코드 삭제 시간';
     *
     * COMMENT ON COLUMN "p_user"."deleted_by" IS 'user_id / 레코드 삭제자';
     *
     * ALTER TABLE "p_user" ADD CONSTRAINT "PK_P_USER" PRIMARY KEY (
     * 	"user_id"
     * );
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true , length = 255)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false , length = 20)
    @Enumerated(value = EnumType.STRING)
    private UserEnumRole role;

    // 사용자 정보 수정
    public void updateUser (String username, String password, String email, String nickname) {
        if (username != null) this.username = username; // null이면 건너뜀
        if (password != null) this.password = password; // null이면 건너뜀
        if (email != null) this.email = email; // null이면 건너뜀
        if (nickname != null) this.nickname = nickname; // null이면 건너뜀
    }

    // 사용자 권한 수정
    public void updateRoleUser (UserEnumRole role) {
        if (role != null) this.role = role;
    }

    // 테스트용 목업
    public static User createForTest(Long userId, String username, String email,
                                     String nickname, UserEnumRole role) {
        User user = User.builder()
                .username(username)
                .password("encoded")
                .email(email)
                .nickname(nickname)
                .role(role)
                .build();
        // 리플렉션으로 userId 주입
        try {
            var field = User.class.getDeclaredField("userId");
            field.setAccessible(true);
            field.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

}
