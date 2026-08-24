# Admin Web 작업 지침

- API 접근 코드는 `src/api`에 두고, component는 loading, empty, error 상태를 명시적으로 렌더링한다.
- 설치된 shadcn UI primitive를 직접 수정하지 않고 그대로 조합한다. 동작이나 스타일이 달라야 하면 애플리케이션 전용 wrapper나 custom component를 만든다. 기존 primitive로 해결할 수 없을 때만 CLI로 shadcn component를 추가한다.
- semantic HTML, 눈에 보이는 keyboard focus, label이 있는 control, 충분한 색상 대비를 사용한다.
- route나 button을 숨기는 것을 인가로 사용하지 않는다. 서버가 권한을 최종 강제한다.
- UI 동작을 변경하면 lint, typecheck, unit test, build를 실행한다.
