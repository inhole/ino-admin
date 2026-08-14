# 모듈 경계

현재 의존 방향은 다음과 같다.

```text
apps/admin-server -> features/identity -> modules/common-core
apps/admin-server ---------------------> modules/common-core
```

- `admin-server`는 HTTP API, 보안 filter/JWT 구현, 실행 설정과 migration을 조립한다.
- `identity`는 인증 도메인, 유스케이스, JPA entity/repository를 소유한다.
- `common-core`는 앱 또는 feature를 참조하지 않는다.
- feature 간 직접 의존과 역방향 의존은 허용하지 않는다.

Gradle project dependency가 이 방향을 물리적으로 강제한다. `verifyModuleDependencies`가 금지된 project dependency를 검사하며 `architectureTest` 실행 시 자동으로 함께 실행된다. 신규 feature는 독립 Gradle project로 추가하며 실행 앱에서만 조립한다.
