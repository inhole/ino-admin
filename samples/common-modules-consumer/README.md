# Common modules consumer fixture

`admin-server`와 독립된 Spring Boot application context에서 Phase 8 공통 모듈의 소비 계약을 검증합니다.

```powershell
.\gradlew.bat :samples:common-modules-consumer:test
```

consumer는 사용할 `common-*` 모듈을 명시적으로 의존하고 `Clock`, `AuditWriter` 같은 애플리케이션 확장점을 제공합니다.
웹 auto-configuration을 사용하는 이 fixture는 servlet runtime을 위해 `spring-boot-starter-web`을 선택합니다.
실제 HTTP endpoint, 업무 DTO, JPA entity와 production 실행 설정은 포함하지 않습니다.
