# common-audit

애플리케이션이 감사 이벤트를 기록 저장소와 분리해 전달하기 위한 최소 계약 모듈입니다.

- `AuditCommand`: actor, login account, action, resource, result, HTTP·요청 추적 정보
- `AuditResult`: `SUCCESS`, `FAILURE`
- `AuditWriter`: consumer가 DB, 메시지 또는 외부 저장소 adapter로 구현하는 기록 port

route별 action 매핑, 민감정보 allowlist·masking, JPA entity와 검색·보존 정책은 consumer에 남겨야 합니다.
이 모듈은 Spring 또는 영속성 의존성을 요구하지 않으며 auto-configuration을 제공하지 않습니다.
