# Identity Feature

사용자 인증, 로그인 잠금, 비밀번호 정책, refresh token 수명주기와 초기 관리자 생성을 담당한다.

## 공개 경계

- `LoginService`: 로그인 및 현재 사용자 조회
- `PasswordChangeService`: 본인 비밀번호 변경
- `RefreshTokenService`: token rotation과 로그아웃
- `AccessTokenIssuer`: 실행 애플리케이션이 구현하는 access token 발급 port
- 인증 관련 예외와 configuration properties

JPA entity와 repository는 패키지 외부에 공개하지 않는다. REST controller, JWT 구현, HTTP DTO와 Flyway migration은 실행 애플리케이션인 `admin-server`에 남긴다.

## 의존성

```text
apps/admin-server -> features/identity -> modules/common-core
```

이 모듈은 `apps/*` 또는 다른 `features/*`에 의존할 수 없다.
