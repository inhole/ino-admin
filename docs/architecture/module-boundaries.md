# 모듈 경계

현재 의존 방향은 다음과 같다.

```text
apps/admin-server/{identity,menu,file,...} -> modules/common-*
samples/common-modules-consumer ----------> modules/common-*
```

- `admin-server`는 HTTP API, 실행 설정과 migration뿐 아니라 identity, menu, file 등 앱 전용 업무 패키지를 소유한다.
- 각 업무 패키지는 기존 `api`, `application`, `domain`, `infrastructure` 하위 경계를 유지한다.
- 앱 전용 controller, DTO, entity와 정책은 `common-*`로 이동하지 않는다.
- `common-*`는 `apps/*`를 참조하지 않는다.
- 독립 consumer fixture는 `common-*`만 project dependency로 사용할 수 있다.
- `common-excel`은 POI 타입을 감춘 XLSX 표 reader/writer와 셀 안전성만 제공한다. 컬럼 정의, 파일 제한, 행별 업무 검증과 오류 문구는 `admin-server`가 소유한다.

`verifyModuleDependencies`는 common 모듈의 앱 역의존, consumer fixture의 비공통 의존과 별도 `features:*` Gradle project 재도입을 검사한다. `PackageBoundaryTest`는 identity/menu/file 업무 패키지 간 직접 참조와 domain의 외부 계층 역참조를 ArchUnit으로 검사한다. 두 검증은 `architectureTest` 실행 시 함께 동작한다. 신규 업무 기능은 먼저 `apps/admin-server` 내부 패키지의 vertical slice로 구현하고, 검증된 범용 계약만 `common-*`로 추출한다.
