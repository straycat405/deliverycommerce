<div align="center">

# 🍽️ Delivery Commerce API

### AI 검증 비즈니스 프로젝트
**음식점 주문 관리 플랫폼 Backend API**

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![AWS](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)

</div>

## 📌 프로젝트 소개

> 프로젝트 명칭은 **Delivey Commerce API** 입니다.
> 음식점 등록부터 주문, 결제, 리뷰까지 전반적인 배달 서비스 흐름을 구현합니다.
> AI API 연동을 통한 메뉴 추천, 상품 설명 자동 생성 기능 등을 제공합니다.

---

## 👥 7조 - **밥조** 🍚 구성

<div align="center">

| 이름 | 담당 | GitHub |
|:---:|:---:|:---:|
| 김진영 | 조장 - Order,OrderItem | [@github](https://github.com/Jinyoung-Kim96) |
| 박동진 | 부조장 - User, Auth, Git | [@github](https://github.com/straycat405) |
| 박지훈 | 멤버 - Store, Cart, CartItem | [@github](https://github.com/jihxonx) |
| 장윤호 | 멤버 - Payment, Review, 발표 | [@github](https://github.com/githyj-jang) |
| 윤명우 | 멤버 - Product, AI | [@github](https://github.com/Lazernes) |

</div>

---

## 🛠 기술 스택

### Backend
| 분류 | 기술 |
|:---:|:---:|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| ORM | Spring Data JPA |
| Security | Spring Security + JWT |
| Build | Gradle |
| Docs | Swagger |

### Database
| 분류 | 기술 |
|:---:|:---:|
| DB | PostgreSQL |
| Query | QueryDSL |

### Infra
| 분류 | 기술 |
|:---:|:---:|
| Deploy | AWS EC2 (Free tier) |
| CI/CD | GitHub Actions |

---

## 🏗️ 아키텍처

```
TBD
```

---

## 📊 ERD

> TBD

---

## 📁 패키지 구조
```
TBD
```

---

## ⚙️ 서비스 실행 방법

### 사전 요구사항
```
- Java 17
- PostgreSQL 설치 및 실행
- .env 파일 설정
...

(TBD)
```

### 환경변수 설정
```bash
# .env 파일 생성 후 아래 내용 입력
DB_NAME=deliverycommerce
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=

...

(TBD)
```

### 실행
```bash
# 1. 프로젝트 clone
git clone https://github.com/straycat405/deliverycommerce.git

# 2. develop 브랜치로 이동
git checkout develop

# 3. 프로젝트 빌드
./gradlew build

# 4. 실행
./gradlew bootRun

...

(TBD)
```

---

## 📮 API 문서

> 서버 실행 후 (포트번호 8080 기준) 아래 주소에서 확인 가능합니다.
```
http://localhost:8080/swagger-ui/index.html
```

---

## 📋 주요 기능

### 👤 사용자 (User / Auth)
- 회원가입 / 로그인
- JWT 기반 인증 / 인가
- 사용자 정보 CRUD + Search
- 권한 관리 (CUSTOMER / OWNER / MANAGER / MASTER)

### 🏪 가게 (Store)


### 🍱 상품 (Product)


### 📦 주문 (Order)


### 💳 결제 (Payment)


### ⭐ 리뷰 (Review)


### 🤖 AI (AI)


---

<div align="center">

**밥조** 🍚 | AI 검증 비즈니스 프로젝트

</div>
