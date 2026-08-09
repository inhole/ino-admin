# ADR-002: JWT access token과 서버 관리 refresh token

- 상태: 승인
- 일자: 2026-08-09

## 결정

수명이 짧은 stateless JWT access token을 사용한다. refresh token은 hash만 저장하고 rotation하여 세션 폐기와 token family 재사용 탐지가 가능하게 한다.

## 결과

Phase 2에서 인증을 활성화하기 전에 issuer, audience, signing key rotation, refresh token 저장, cookie/CSRF 정책과 재사용 탐지 시 대응을 정의해야 한다.
