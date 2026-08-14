# Identity Feature

사용자 인증, 로그인 잠금, 비밀번호 정책, refresh token 수명주기와 초기 관리자 생성을 담당한다.

## 디렉터리 구조

```text
identity/
├─ api/                         # 실행 앱이 사용하는 유스케이스 계약과 예외
├─ application/                 # 유스케이스 구현
│  └─ port/                     # 실행 앱이 구현하는 출력 port
├─ domain/                      # User, token, 비밀번호 정책
├─ infrastructure/persistence/  # Spring Data JPA repository
├─ bootstrap/                   # 초기 관리자 생성
└─ config/                      # identity 설정 properties
```

## 공개 경계

- `LoginUseCase`: 로그인 및 현재 사용자 조회
- `PasswordChangeUseCase`: 본인 비밀번호 변경
- `RefreshTokenUseCase`: token rotation과 로그아웃
- `AccessTokenIssuer`: 실행 애플리케이션이 구현하는 access token 발급 port
- `api`의 인증 관련 예외

실행 애플리케이션은 `api`와 `application.port`만 사용한다. JPA entity와 repository는 identity 내부 구현으로 취급한다. REST controller, JWT 구현, HTTP DTO와 Flyway migration은 실행 애플리케이션인 `admin-server`에 남긴다.

## 의존성

```text
apps/admin-server -> features/identity -> modules/common-core
```

이 모듈은 `apps/*` 또는 다른 `features/*`에 의존할 수 없다.
