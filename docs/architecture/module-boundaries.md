# 모듈 경계

현재 의존 방향은 다음과 같습니다.

```text
ino-admin/apps/admin-server/{identity,menu,file,...}
    -> com.ino.spring.modules:common-*:0.1.0
    -> inhole/ino-spring-modules
```

- `admin-server`는 HTTP API, 실행 설정과 migration 및 identity, menu, file 등 앱 전용 업무 패키지를 소유합니다.
- 앱 전용 controller, DTO, entity와 정책은 공통 artifact로 이동하지 않습니다.
- `common-file`은 Local adapter를 제공하고, S3가 필요한 이 앱은 `common-file-s3`를 명시적으로 추가합니다.
- `common-audit`의 저장소 독립 계약만 사용하며 servlet 문맥과 저장 필드 allowlist는 `admin-server`가 소유합니다.
- `common-excel`의 POI 비노출 API를 사용하고 컬럼·파일 제한·업무 검증은 앱이 소유합니다.
- 공통 모듈 소스, sample consumer, publishing 및 API baseline은 `ino-spring-modules` 저장소가 소유합니다.

`verifyModuleDependencies`는 제거된 `:modules:common-*` project dependency의 재도입을 차단합니다. `PackageBoundaryTest`는 앱 내부 feature 및 계층 경계를 검사합니다. 공통 artifact 자체의 호환성과 독립 소비 검증은 `ino-spring-modules` CI가 담당합니다.
