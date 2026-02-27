package com.babjo.deliverycommerce.user.entity;

import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
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

    @Column(nullable = false , length = 50)
    private String nickname;

    @Column(nullable = false , length = 20)
    @Enumerated(value = EnumType.STRING)
    private UserEnumRole role;

}
