# common-security

HS256 JWT 발급·검증과 `permissions` claim을 Spring Security authority로 변환하는 기반 모듈입니다.

## 설정

- `app.jwt.secret`: Base64로 인코딩한 32바이트 이상의 비밀키, 필수
- `app.jwt.issuer`: 발급자, 기본값 `ino-admin`
- `app.jwt.audience`: 대상 audience, 기본값 `ino-admin-web`
- `app.jwt.access-token-ttl`: access token 수명, 기본값 `15m`

consumer는 `Clock` bean을 제공해야 합니다. auto-configuration은 `JwtEncoder`, `JwtDecoder`,
`JwtTokenService`, `JwtPermissionAuthenticationConverter`를 동일 타입 bean이 없을 때만 등록합니다.
URL별 허용 정책, 인증 API, 사용자·역할 조회는 애플리케이션에 남겨야 합니다.
