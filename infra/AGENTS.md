# 인프라 작업 지침

- 로컬 기본값에는 민감 정보를 포함하지 않고, 외부에 노출하는 모든 host port를 문서화한다.
- 정상 시작 명령에 production credential이나 파괴적인 volume reset 동작을 포함하지 않는다.
- service readiness를 확인하는 health check를 추가하고, Compose 변경은 `docker compose config`로 검증한다.
