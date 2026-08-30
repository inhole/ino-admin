# 공통 모듈 버전과 publishing 전략

## 좌표와 버전

Phase 8 공통 모듈은 `com.ino.admin:<module-name>:<version>` Maven 좌표를 사용하고 모든 모듈을 같은 버전으로 배포합니다.
기본 개발 버전은 `0.1.0-SNAPSHOT`이며, 검증된 release 후보는 `-PreleaseVersion=MAJOR.MINOR.PATCH`로 생성합니다.

- MAJOR: 기존 consumer 수정 없이는 사용할 수 없는 public API 또는 설정 계약 변경
- MINOR: 하위 호환되는 기능, adapter 또는 확장점 추가
- PATCH: 하위 호환 버그·보안 수정과 내부 리팩터링
- SNAPSHOT: `dev`에서만 사용하는 비고정 개발 artifact

`0.x` 기간에도 public API 제거·이름 변경·기본 동작 변경은 MINOR 이상으로 올리고 migration note를 작성합니다.

## repository-local staging 검증

```powershell
.\gradlew.bat verifyCommonModulePublications
.\gradlew.bat "-PreleaseVersion=0.1.0" verifyCommonModulePublications
```

이 task는 `build/staging-repository`에 여섯 모듈의 JAR/POM을 실제 발행하고, 버전 일치와 `apps/*`·`features/*` 역의존 부재를 검사합니다.
생성물은 검증용이며 Git에 커밋하지 않습니다.

consumer 좌표 예시는 다음과 같습니다.

```kotlin
dependencies {
    implementation("com.ino.admin:common-core:0.1.0")
    implementation("com.ino.admin:common-web:0.1.0")
}
```

## 외부 repository 승격 경계

Phase 8에서는 외부 repository credential이나 업로드 workflow를 두지 않습니다. Phase 10에서 다음 조건을 충족한 tag만 원격 Maven repository로 승격합니다.

1. tag `vMAJOR.MINOR.PATCH`와 `releaseVersion`이 일치한다.
2. Main Integration CI와 빈 consumer 설치 리허설이 통과한다.
3. 동일 commit에서 생성한 staging artifact를 승격하며 다시 빌드하지 않는다.
4. 공개 후 같은 버전은 덮어쓰지 않고 수정 시 PATCH 버전을 올린다.
