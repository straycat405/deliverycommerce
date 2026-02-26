# Claude Code 프로젝트 지침

## 프로젝트 개요
- 스파르타 코딩클럽 내일배움캠프 부트캠프 학습용 과제 팀 프로젝트
- 음식 주문 관리 플랫폼 백엔드 API
- 내용 : 음식점들의 배달 주문관리, 결제, 주문 내역 관리 기능을 제공

## 🚨 필수 규칙
1. 컴포넌트 / class  `PascalCase`
2. 폴더명  `carmelCase`
3. 파일 명 *(컴포넌트 제외)*   `carmelCase`
4. 변수, 함수  `carmelCase`
5. 파라미터  `carmelCase`
6. 상수  `BIG_SNAKE_CASE`

## 작업 분배 상황
- 박동진 - USER / AUTH 도메인
- 현재 이슈
- 서비스 이용 전반에 필요한 USER / AUTH 도메인 관련 기능구현
  JWT , Spring Security Filter , Redis를 혼합한 인증 , 보안 기능 구현
  role에 따른 권한 차등 부여된 API 설계
  🎯 목표
  User 엔티티 및 Repository 구성
  회원가입 API
  로그인 / JWT발급 / Redis 연동
  Spring Security 필터 체인 구성
  로그아웃 / 토큰 재발급
  사용자 CRUD (조회 / 수정 / 탈퇴 / 권한수정)
  🧩 기능 상세

User 엔티티 (p_user 테이블 매핑, BaseEntity 상속)

Role ENUM 정의 (CUSTOMER / OWNER / MANAGER / MASTER)

회원가입 — 유효성 검사, 중복 검사, BCrypt 암호화

로그인 — JWT Access/Refresh 토큰 발급, Redis refresh:{userId} 저장

JWT 필터 — 요청마다 토큰 검증 + DB role 조회로 권한 체크

로그아웃 — Access Token blacklist 등록, Refresh Token Redis 삭제

토큰 재발급 — Refresh Token 검증 후 Access Token 재발급

사용자 단건 조회 / 목록 검색 (페이지네이션)

사용자 수정 — 본인 / 관리자 분리

회원 탈퇴 — Soft Delete, 본인 / 관리자 분리

권한 수정 — MASTER 전용
🏗️ 설계
사용 기술 : Spring Boot 3.x, Spring Data JPA, Spring Security, PostgreSQL, Redis, JWT (jjwt), Lombok

DB 설계 : p_user 테이블 (ERD , 테이블명세서 참고)

username UNIQUE / email UNIQUE
Soft Delete : deleted_at, deleted_by
Audit : created_at, created_by, updated_at, updated_by
인증 흐름 요약

로그인
→ Access Token (15분) : Response Body 반환
→ Refresh Token (7일) : HttpOnly Cookie 저장, Redis에 refresh:{userId} 키로 저장

요청마다
→ JWT 필터에서 Access Token 검증
→ Redis / DB에서 role 유효성 재확인 (Stale Token 방지)

로그아웃
→ Access Token → Redis blacklist:{token} 등록 (TTL = 토큰 남은 만료시간)
→ Redis refresh:{userId} 삭제
💻 API 구조
Method	Endpoint	권한	설명
POST	/v1/users/signup	ALL	회원가입
POST	/v1/users/login	ALL	로그인
POST	/v1/users/logout	로그인 유저	로그아웃
POST	/v1/users/reissue	로그인 유저	토큰 재발급
GET	/v1/users/{userId}	본인 / MANAGER / MASTER	단건 조회
GET	/v1/users	MANAGER / MASTER	목록 검색
PATCH	/v1/users/me	본인	본인 정보 수정
PATCH	/v1/users/{userId}	MANAGER / MASTER	관리자 수정
DELETE	/v1/users/me	본인	본인 탈퇴
DELETE	/v1/users/{userId}	MANAGER / MASTER	관리자 탈퇴
PATCH	/v1/users/{userId}/role	MASTER	권한 수정
Image
🧪 테스트 계획

정상 케이스 : 회원가입 → 로그인 → API 호출 전체 플로우

중복 데이터 : username / email 중복 가입 시도

권한 오류 : CUSTOMER가 관리자 전용 API 호출 시 403 반환

토큰 만료 : 만료된 Access Token으로 요청 시 401 반환

Stale Token : 권한 변경 후 기존 토큰으로 요청 시 차단 확인

로그아웃 후 : 블랙리스트 토큰으로 재요청 시 401 반환
📈 기대 효과
전 도메인의 인증/인가 기반 확립으로 팀원 작업 블로킹 해소
role 기반 접근 제어로 CUSTOMER / OWNER / MANAGER / MASTER 권한 분리
Redis 기반 토큰 관리로 즉시 권한 무효화 가능 (Stale Token 문제 해결)
🔗 참고 자료
API 명세서 : Notion 팀 페이지 API명세 참고
ERD : 테이블 명세서 (p_user)
하위 브랜치
feature/user-domain
feature/user-signup
feature/user-login
feature/user-security
feature/user-token
feature/user-crud




### 코드 생성 / 수정 금지
- 이 프로젝트는 학습 목적이므로 코드를 직접 생성하거나 수정하지 않는다
- 코드 작성은 반드시 개발자가 직접 한다
- 코드 예시가 필요한 경우 설명과 함께 제안만 한다

### 허용 범위
- 코드 리뷰 및 피드백
- 개념 설명 및 학습 가이드
- 오류 원인 분석 및 해결 방향 제시
- 작업 순서 및 설계 방향 제안

## 기술 스택
- 백엔드 : Java 17, Spring boot 3.x
- 버전 관리 : GitHub
- 빌드 툴 : Gradle
- DB : PostgreSQL
- ORM : Spring Data JPA
- 보안 : Spring Security + JWT
- 캐시/세션 : Redis
- 배포 환경 : AWS EC2 (Free tier) , ElastiCache
- CI/CD: GitHubAction
- 아키텍쳐 : Monolithic Application
- 암호화 : BCrypt

## 브랜치 전략
- main : 배포용
- develop : 개발 기준 브랜치
- feature/* : 기능 단위 브랜치

## 커밋 컨벤션
- Init	초기 세팅시에만 사용 (패키지 설치, eslint/ prettier 작성)
- Feat	새로운 기능 추가 (새로운 구현)
- Fix	버그 수정, 기능 수정
- Docs	문서 추가, 수정, 삭제
- Refactor	코드 리팩토링
- Comment	필요한 주석 추가 및 변경
- Rename	파일 또는 폴더 명을 수정하거나 옮기는 작업만인 경우
- Remove	파일을 삭제하는 작업만 수행한 경우
- !HOTFIX	급하게 치명적인 버그를 고쳐야 하는 경우
- Test	테스트 코드, 리팩토링 테스트 코드 추가
- Chore	(코드의 수정 없이) 그 외 기타 수정
```

---

## 동작 방식
```
Claude Code 실행 시
→ 프로젝트 루트의 CLAUDE.md 자동 감지
→ 내용 숙지 후 지침에 따라 동작