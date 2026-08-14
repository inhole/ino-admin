## 변경 사항

-

## 배경과 영향

- 해결하려는 문제:
- 사용자/운영 영향:

## 검증

- [ ] Backend: `./gradlew clean test integrationTest architectureTest`
- [ ] Frontend: `npm run lint && npm run typecheck && npm test && npm run build`
- [ ] Infrastructure: `docker compose -f infra/compose.yaml config`
- [ ] 실행하지 못한 검증과 이유를 아래에 기록했다.

## 변경 위험

- [ ] API/DB/설정 변경과 호환성을 문서화했다.
- [ ] 인증·인가·민감 정보 영향을 검토했다.
- [ ] migration 또는 장애 발생 시 복구 방법을 확인했다.
- [ ] 관련 없는 변경을 포함하지 않았다.

## Stacked PR

- 선행 PR: 해당 없음
- [ ] base가 `main`이 아니면 `stacked` 라벨과 merge 순서를 지정했다.
