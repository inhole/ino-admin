# Menu Feature

현재 사용자의 세부 권한에 따라 관리자 메뉴 tree를 계산한다. route와 icon key는 서버 catalog가 제공하고, 프론트는 허용된 메뉴만 렌더링한다.

공개 경계는 `MenuQueryUseCase`이며 다른 feature에 의존하지 않는다. 영속화와 메뉴 CRUD는 catalog 경계가 검증된 후 Flyway migration을 포함한 후속 slice에서 확장한다.
