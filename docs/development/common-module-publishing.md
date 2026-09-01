# 공통 모듈 소비 정책

공통 Spring 모듈의 소스·릴리스·호환성 검증은 별도 저장소 [inhole/ino-spring-modules](https://github.com/inhole/ino-spring-modules)가 소유합니다. 이 저장소는 GitHub Packages에 발행된 고정 버전만 소비합니다.

## Maven 좌표

```kotlin
dependencies {
    implementation("com.ino.spring.modules:common-core:0.1.0")
    implementation("com.ino.spring.modules:common-web:0.1.0")
}
```

일곱 artifact는 동일 버전을 사용합니다. 버전 변경은 `ino-spring-modules`의 release note와 호환성 검증 결과를 확인한 뒤 한 번에 적용합니다.

## GitHub Packages 인증

로컬 Gradle은 `GITHUB_ACTOR`/`GITHUB_TOKEN` 또는 `~/.gradle/gradle.properties`의 `gpr.user`/`gpr.token`을 사용합니다. 토큰에는 `read:packages` 권한이 필요하며 저장소나 `.env`에 기록하지 않습니다.

```properties
gpr.user=<github-user>
gpr.token=<read-packages-token>
```

Dev CI는 저장소의 `GITHUB_TOKEN`과 `packages: read` 권한으로 artifact를 해석합니다. 공통 모듈의 publishing, API baseline, 독립 consumer 검증은 `ino-spring-modules` CI에서 수행합니다.
