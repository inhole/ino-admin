# ADR-002: JWT access token과 서버 관리 refresh token

- 상태: 승인
- 일자: 2026-08-09

## 결정

수명이 짧은 stateless JWT access token을 사용한다. refresh token은 hash만 저장하고 rotation하여 세션 폐기와 token family 재사용 탐지가 가능하게 한다.

access token은 다음 기준을 적용한다.

- HS256 알고리즘과 Base64 인코딩된 최소 256-bit secret을 사용한다.
- secret은 환경 변수 또는 배포 환경의 secret manager로만 주입한다.
- 기본 수명은 15분이며 `issuer`, `audience`, 만료 시각과 서명을 모두 검증한다.
- subject에는 사용자 UUID만 저장하고 이메일·표시 이름 등 개인정보 claim은 넣지 않는다.
- 로그인 실패는 사용자 존재 여부와 계정 상태를 구분하지 않는 동일한 응답을 사용한다.

관리자 웹은 access token과 refresh token을 탭 단위 `sessionStorage`에 저장한다. 브라우저를 새로고침하면 저장된 refresh token을 즉시 rotation하고 `/api/v1/auth/me`로 인증 상태를 복구한다. `localStorage`처럼 브라우저 재시작 후까지 token을 유지하지 않으며, 로그아웃이나 refresh 실패 시 두 token을 모두 제거한다. 여러 API 요청이 동시에 401을 받더라도 refresh 요청은 하나만 실행한다.

이 모델의 token은 JavaScript에서 접근할 수 있으므로 Content Security Policy와 XSS 방지가 보안 전제다. 향후 refresh token을 HttpOnly cookie로 전환할 경우 SameSite, CORS, CSRF 정책을 함께 재검토해야 한다.

## 결과

현재 bearer header 기반 access token은 stateless이므로 CSRF를 비활성화한다. 서버는 refresh token 원문 대신 hash를 저장하고 rotation과 token family 재사용 탐지를 수행한다. 프론트엔드는 보호 route, 인증 상태 복구와 만료 시 단일 refresh 재시도를 제공한다.
