# INO Admin Web

React, Vite, Tailwind CSS와 shadcn/ui 기반 관리자 웹입니다.

UI/UX 개선 단계와 화면별 적용 범위는 [`docs/architecture/admin-ui-ux-roadmap.md`](../../docs/architecture/admin-ui-ux-roadmap.md)를 따릅니다.

## shadcn/ui 프리셋

- 공식 shadcn CLI 프리셋 `b1aIuQ2XC`를 사용합니다.
- 구성은 Base UI 기반 `luma` 스타일, `stone` 기본색, `blue` 테마·차트, `remixicon`, `Raleway` 가변 폰트, 큰 반경, 반투명 기본 메뉴입니다.
- 설정의 기준 파일은 `components.json`, 전역 테마 토큰과 폰트의 기준 파일은 `src/styles.css`입니다.
- 프리셋을 다시 적용할 때는 `apps/admin-web`에서 아래 명령을 실행합니다. `--force --reinstall`은 기존 공식 UI 컴포넌트 소스를 덮어쓰므로 로컬 변경을 먼저 확인해야 합니다.

```text
npx shadcn@latest init --preset b1aIuQ2XC --template vite --force --reinstall
npx shadcn@latest info --json
```

- 새 UI는 먼저 `npx shadcn@latest search @shadcn -q "<검색어>"`로 공식 컴포넌트를 찾고, `npx shadcn@latest docs <컴포넌트>`로 Base UI 조합을 확인한 뒤 `npx shadcn@latest add <컴포넌트>`로 설치합니다.
- 설치된 컴포넌트를 갱신할 때는 `add --dry-run`과 `add --diff <파일>`로 변경을 검토한 뒤 로컬 확장을 보존합니다.
- 기능 화면은 `src/components/ui` 소스를 직접 변형하기보다 공식 variant와 조합을 우선하고, 색상은 `src/styles.css`의 의미 기반 토큰을 사용합니다.

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
