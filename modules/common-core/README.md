# common-core

HTTP, Security, JPA와 무관한 오류 descriptor, 업무 예외와 페이지 응답 기반 타입을 제공합니다.

```kotlin
implementation("com.ino.admin:common-core:<version>")
```

- `ErrorDescriptor`: consumer 오류 catalog가 구현하는 최소 code/message 계약
- `BusinessException`: 문자열 또는 `ErrorDescriptor`로 생성하는 전송 계층 독립 예외
- `PageResponse`: 불변 페이지 응답 기반 타입

업무별 오류 enum과 현지화 메시지, controller, entity와 화면 DTO는 consumer에 남겨야 합니다.
버전 및 artifact 검증 절차는 `docs/development/common-module-publishing.md`를 따릅니다.
