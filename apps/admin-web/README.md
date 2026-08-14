# INO Admin Web

React, Vite, Tailwind CSS와 shadcn/ui 기반 관리자 웹입니다.

## 화면 설정

- 테마는 `light`, `dark`, `system`을 지원하며 `ino-admin.theme` 로컬 저장소 키에 보관합니다.
- 색상과 간격은 `src/styles.css`의 의미 기반 CSS 변수만 확장합니다. 기능 화면에서 고정 색상을 직접 사용하지 않습니다.
- 반응형 화면은 최소 320px부터 지원하며, 모바일에서는 오버레이 메뉴와 카드형 데이터 표현을 우선합니다.

## 다국어 확장

- 정적 UI 문구는 `src/i18n/resources.ts`의 기능별 namespace에 둡니다.
- 새 언어는 같은 키 구조의 리소스를 등록하고 `supportedLngs`에 locale을 추가합니다.
- 날짜, 시간, 숫자와 파일 크기는 `src/i18n/format.ts`의 `Intl` 포매터를 사용합니다.
- 서버에서 반환하는 동적 메뉴명, 역할명과 업무 오류 메시지는 서버 원문을 표시합니다.

## 검증

```text
npm run lint
npm run typecheck
npm test
npm run build
npm run test:e2e
```
