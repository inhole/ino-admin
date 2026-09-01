# Staged common modules consumer

이 fixture는 root build가 repository-local staging에 발행한 JAR/POM만 사용해 모든 `common-*` artifact의 조합 소비를 검증합니다. root project dependency는 허용하지 않으며 auto-configuration import, JWT와 파일 설정 binding, 앱 제공 `Clock`·`AuditWriter` 확장점을 Spring Boot context에서 확인합니다.

저장소 root에서 다음 명령을 실행합니다.

```powershell
.\gradlew.bat verifyStagedCommonModuleConsumer
```

외부 artifact repository 배포, production endpoint와 관리자 업무 DTO/entity는 범위에 포함하지 않습니다.
