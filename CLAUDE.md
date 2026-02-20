# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Recorday(Harucut)는 4컷 사진 프레임 편집 서비스의 Spring Boot 백엔드입니다. 사용자는 OAuth2 또는 이메일/비밀번호로 인증하고, 프레임을 생성·편집하며, 구성 요소(사진/스티커/텍스트)를 배치할 수 있습니다.

## Build & Run Commands

```bash
./gradlew clean build              # 전체 빌드
./gradlew test                     # 전체 테스트 실행
./gradlew test --tests "ClassName" # 특정 테스트 클래스 실행
./gradlew bootRun                  # 애플리케이션 실행
./gradlew bootJar                  # 실행 가능한 JAR 빌드
```

## Tech Stack

- **Spring Boot 4.0.0**, Java 17
- **Spring Security** with JWT (JJWT 0.11.5) + OAuth2 (Kakao, Naver, Google)
- **Spring Data JPA** (MySQL 8.0 production, H2 in-memory test)
- **Redis** — refresh token, 인증 코드 저장
- **AWS S3** — 파일 업로드/다운로드
- **SpringDoc OpenAPI** — Swagger UI (`/swagger-ui/`)
- **NanoID** — public_id 생성 (12자)

## Architecture

루트 패키지: `com.recorday.recorday`

### 도메인 모듈

| 모듈 | 역할 |
|------|------|
| `auth` | JWT 발급/검증, OAuth2 핸들러, 로컬 로그인/회원가입, 이메일 인증, 계정 삭제 스케줄러 |
| `frame` | 프레임 CRUD, FrameComponent 관리 (PHOTO/STICKER/TEXT) |
| `user` | 사용자 프로필 관리 |
| `storage` | S3 파일 업로드 전략 패턴 |
| `mail` | 이메일 발송 (Thymeleaf 템플릿) |

### 공통 모듈

| 모듈 | 역할 |
|------|------|
| `config` | SecurityConfig, JpaConfig, RedisConfig, JwtConfig, S3Config, SwaggerConfig |
| `exception` | GlobalExceptionHandler, BusinessException, ErrorCode 인터페이스 |
| `util.entity` | BaseEntity(감사), BasePublicIdEntity(NanoId 자동 생성) |
| `common` | 커스텀 어노테이션, AOP, LockStrategy 등 |

### 엔티티 계층

```
BaseEntity (createdAt/updatedAt JPA 감사)
  └── BasePublicIdEntity (public_id: 12자 NanoId, unique index)
        ├── User (provider, email, password, userStatus, userRole)
        ├── Frame (title, description, frameType, background JSON)
        └── FrameComponent (type, x/y/width/height/scale/rotation/zIndex, styleJson)
```

- **User ↔ Frame**: 1:N (cascade delete, orphan removal)
- **Frame ↔ FrameComponent**: 1:N (cascade delete, orphan removal)
- User에 `(provider, email)` 및 `(provider, providerId)` unique constraint 존재

### 인증 흐름

- JWT 기반 stateless 인증 (access token 1일, refresh token 14일)
- OAuth2 로그인 성공 시 `CustomOAuth2SuccessHandler`가 쿠키로 토큰 전달
- 로컬 로그인은 `LocalAuthController`에서 처리
- Refresh token 재발급은 `RefreshTokenController`에서 처리
- Redis에 refresh token 저장, 쿠키 기반 전송 지원

### 주요 API 경로

- `/api/harucut/login|register|reissue` — 인증
- `/api/auth/user/frame/**` — 프레임 CRUD (인증 필요)
- `/login/oauth2/code/{provider}` — OAuth2 콜백

## Configuration Profiles

| 프로필 | DB | Redis | 용도 |
|--------|-----|-------|------|
| default (`application.yml`) | MySQL localhost:3306 | localhost:6379 | 로컬 개발 |
| test (`application-test.yml`) | H2 in-memory | — | 테스트 |
| deploy (`application-deploy.yml`) | mysql:3306 (Docker) | redis:6379 (Docker) | 배포 |

## Testing Conventions

- JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- 테스트 프로필에서 H2 in-memory DB 사용 (ddl-auto: create-drop)
- 테스트 파일 위치: `src/test/java/com/recorday/recorday/`

## Deployment

Docker Compose로 배포: app(harucut-app), MySQL(harucut-mysql), Redis(harucut-redis) 컨테이너.
프론트엔드 URL: 개발 `http://localhost:3000`, 프로덕션 `https://harucut.com`.

## Development Principles

### 1. 관심 분리 (Separation of Concerns)
- 컨트롤러는 요청/응답 처리만 담당하고, 비즈니스 로직은 반드시 서비스 레이어에 둔다.
- 컨트롤러에서 직접 Enum 변환, 데이터 가공 등의 로직을 수행하지 않는다.

### 2. SOLID 원칙
- 객체지향 5원칙(SRP, OCP, LSP, ISP, DIP)을 준수한다.
- 인터페이스 분리를 통해 역할별 서비스를 명확히 나눈다 (예: ProfileCreateService, ProfileManageService).
- 의존성 역전을 통해 구현이 아닌 추상화에 의존한다.

### 3. 디자인 패턴 적용
- **전략 패턴**: 코드 추가/변경이 용이하도록 행위를 캡슐화한다.
- **파사드 패턴**: 복잡한 하위 시스템을 단순한 인터페이스로 감싸 유지보수성을 높인다.
- **팩토리 패턴**: 객체 생성 로직을 분리하여 생성을 용이하게 한다.
- 디자인 패턴을 적재적소에 사용하되, 과도한 추상화는 지양한다.

### 4. 테스트
- 단위 테스트와 통합 테스트를 반드시 작성한다.
- 테스트 코드를 통해 수정 사항이 생기더라도 자신 있게 변경할 수 있는 안전망을 확보한다.
- 서비스 레이어는 단위 테스트, 컨트롤러는 통합 테스트를 기본으로 한다.
