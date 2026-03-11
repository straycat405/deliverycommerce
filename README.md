<div align="center">

# 🍽️ Delivery Commerce API

### AI 검증 비즈니스 프로젝트
**음식점 주문 관리 플랫폼 Backend API**

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![AWS](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)

</div>

---

## Swagger API Docs
### http://3.39.35.239:8080/swagger-ui/index.html

---

## 📌 프로젝트 소개

>  **Delivery Commerce API**는
> 음식점 등록부터 주문, 결제, 리뷰까지 전반적인 배달 서비스 흐름을 구현합니다.
> Google Gemini AI 연동을 통한 상품 설명 자동 생성 기능을 제공합니다.

---

## 👥 7조 - **밥조** 🍚 구성

<div align="center">

| 이름 | 담당 | GitHub |
|:---:|:---:|:---:|
| 김진영 | 조장 - Order, OrderItem | [@Jinyoung-Kim96](https://github.com/Jinyoung-Kim96) |
| 박동진 | 부조장 - User, Auth, Git | [@straycat405](https://github.com/straycat405) |
| 박지훈 | 멤버 - Store, Cart, CartItem | [@jihxonx](https://github.com/jihxonx) |
| 장윤호 | 멤버 - Payment, Review, 발표 | [@githyj-jang](https://github.com/githyj-jang) |
| 윤명우 | 멤버 - Product, AI | [@Lazernes](https://github.com/Lazernes) |

</div>

---

## 🛠 기술 스택

### Backend
| 분류 | 기술 |
|:---:|:---:|
| Language | Java 17 |
| Framework | Spring Boot 3.5.x |
| ORM | Spring Data JPA |
| Query | QueryDSL |
| Security | Spring Security + JWT |
| Cache | Redis |
| Build | Gradle |
| Docs | Swagger (SpringDoc OpenAPI) |
| Logging | Logback, p6spy |

### Database
| 분류 | 기술 |
|:---:|:---:|
| DB | PostgreSQL 16 |
| Cache / Session | Redis 7 |

### Infra
| 분류 | 기술 |
|:---:|:---:|
| Server | AWS EC2 (Free tier) |
| Container | Docker, Docker Compose |
| CI/CD | GitHub Actions + Docker Hub |

---

## 🏗️ 아키텍처

![Image](https://github.com/user-attachments/assets/718684c9-483a-4366-a356-952d3d9e2b99)

### CI/CD Workflow
```
feature/* → develop (PR)  : Build & Test 자동 실행
develop   → main    (PR)  : Build & Test 자동 실행
main merge                : Build & Test → bootJar → Docker 이미지 빌드
                            → Docker Hub push (latest + {sha} 태그)
                            → EC2 SSH 접속 → .env 갱신 → docker pull
                            → docker compose up -d --no-deps app (앱만 재시작, DB/Redis 유지)
```

---

## 📊 ERD

<img width="3590" height="1312" alt="Image" src="https://github.com/user-attachments/assets/bd6e9c75-ca4c-41b3-a201-e5874145fffc" />

---

## 📁 패키지 구조

```
com.babjo.deliverycommerce
├── domain
│   ├── ai            # AI 요청 이력 (AiRequestLog)
│   ├── cart          # 장바구니 (Cart, CartItem)
│   ├── order         # 주문 (Order, OrderItem)
│   ├── payment       # 결제 (Payment, PaymentHistory)
│   ├── product       # 상품 + AI 설명 생성
│   ├── review        # 리뷰 & 평점
│   ├── store         # 가게
│   └── user          # 사용자 & 인증 (Auth)
└── global
    ├── common        # 공통 DTO, BaseEntity, Enum
    ├── exception     # 글로벌 예외 처리 (ErrorCode, CustomException)
    ├── init          # 데이터 초기화 (DataInitializer)
    ├── jpa           # JPA 감사 설정, QueryDSL Config
    ├── jwt           # JWT 유틸, 필터, 핸들러
    ├── redis         # Redis 설정, 유틸, 유저 인증 캐시
    └── security      # Spring Security 설정, UserPrincipal
```

---

## ⚙️ 서비스 실행 방법

### 사전 요구사항
- Java 17
- Docker (로컬 PostgreSQL, Redis 실행용)

### 1. 프로젝트 Clone

```bash
git clone https://github.com/straycat405/deliverycommerce.git
cd deliverycommerce
git checkout develop
```

### 2. 환경변수 설정

`.env.example`을 복사해 `.env` 파일 생성 후 값 입력

```bash
cp .env.example .env
```

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=deliverycommerce
DB_USERNAME=babjo
DB_PASSWORD=babjo1234
JWT_SECRET=your_jwt_secret_key
REDIS_HOST=localhost
REDIS_PORT=6379
SPRING_PROFILES_ACTIVE=dev
GOOGLE_API_KEY=your_google_api_key
GOOGLE_PROJECT_ID=your_google_project_id
```

### 3. PostgreSQL & Redis 실행 (Docker)

```bash
docker compose up -d
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

---

## 📮 API 문서

서버 실행 후 아래 주소에서 Swagger UI 확인 가능합니다.

```
http://localhost:8080/swagger-ui/index.html
```

---

## 📋 주요 기능

### 👤 사용자 (User / Auth)
- 회원가입 / 로그인 (JWT 발급)
- JWT 기반 인증 / 인가
- Redis 캐시를 활용한 권한 검증 (DB 접근 최소화)
- 사용자 정보 CRUD + Search (페이징, 정렬)
- 권한 관리 (`CUSTOMER` / `OWNER` / `MANAGER` / `MASTER`)
- Soft Delete (deletedAt 기반)
- BCrypt 비밀번호 암호화

### 🏪 가게 (Store)
- 가게 등록 / 수정 / 삭제 (`OWNER`)
- 가게 목록 조회 (카테고리 / 이름 필터링 - QueryDSL)
- 가게 상세 조회
- 평균 평점 실시간 반영 (리뷰 연동)
- Soft Delete

### 🍱 상품 (Product)
- 상품 등록 / 수정 / 삭제 (`OWNER`)
- 상품 숨김 / 노출 처리 (hide 필드 별도 관리)
- Google Gemini AI 연동 상품 설명 자동 생성
- Soft Delete

### 🛒 장바구니 (Cart / CartItem)
- 장바구니 조회 / 비우기
- 상품 추가 (동일 상품 존재 시 수량 증가)
- 상품 수량 수정 / 삭제
- Soft Delete

### 📦 주문 (Order)
- 주문 생성 (장바구니 기반, 가격 정합성 검증)
- 본인 주문 목록 조회 (페이징)
- 주문 상세 조회
- 주문 취소 (CUSTOMER - CREATED 상태에서만)
- 주문 거절 (OWNER - CREATED 상태에서만)
- 주문 중도 취소 (OWNER - ACCEPTED, PREPARING, PICKUP_READY 상태에서만)
- 주문 상태 변경 (OWNER - 접수 → 조리중 → 픽업대기 → 픽업완료)
- 주문 내역 Soft Delete

### 💳 결제 (Payment)
- 결제 생성 (READY 상태, 주문 중복 방지)
- 결제 승인 (READY → COMPLETED)
- 결제 실패 처리 (READY → FAILED)
- 결제 취소 (생성 후 5분 이내만 가능, READY/COMPLETED → CANCELED)
- 결제 목록 조회 (QueryDSL 동적 필터링, 페이지 사이즈 10/30/50)
- 결제 Soft Delete
- 상태 변경 이력 Insert-Only 저장 (PaymentHistory)

### ⭐ 리뷰 (Review)
- 리뷰 작성 / 수정 / 삭제
- 평점 1~5점
- 리뷰 등록 시 가게 평균 평점 자동 업데이트
- Soft Delete

### 🤖 AI (Google Gemini)
- 상품 설명 자동 생성 (프롬프트 기반)
- AI 요청/응답 이력 DB 저장
- 요청 텍스트 50자 이하 제한

---

<div align="center">

**밥조** 🍚 | AI 검증 비즈니스 프로젝트

</div>
