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

## 결과

현재 bearer header 기반 access token은 stateless이므로 CSRF를 비활성화한다. signing key rotation, refresh token 저장·rotation·재사용 탐지는 Phase 2의 다음 작업에서 완성해야 한다.
