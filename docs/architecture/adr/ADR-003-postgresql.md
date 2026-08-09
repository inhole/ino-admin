# ADR-003: PostgreSQL을 기본 데이터베이스로 사용

- 상태: 승인
- 일자: 2026-08-09

## 결정

로컬·테스트·운영 환경에서 PostgreSQL을 사용한다. 스키마 변경은 versioned Flyway migration으로만 관리하고 timestamp는 UTC로 저장한다.

## 결과

로컬 개발에는 Compose가 제공하는 PostgreSQL이 필요하다. 통합 테스트는 인메모리 데이터베이스로 대체하지 않고 PostgreSQL 고유 동작을 검증해야 한다.
