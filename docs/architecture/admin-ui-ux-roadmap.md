# Admin Web UI/UX 개선 로드맵

## 목적

INO Admin을 단순 CRUD 화면 모음에서 탐색, 조회, 처리 흐름이 명확한 반응형 B2B 관리자 제품으로 개선한다. `shadcndashboard/shadcndashboard`의 Base UI 호환 구성, `satnaing/shadcn-admin`의 관리자 셸, `marmelab/shadcn-admin-kit`의 CRUD 패턴을 참고하되 외부 템플릿 코드를 직접 복사하지 않고 공식 shadcn/ui 컴포넌트를 CLI로 설치·조합한다.

기존 REST API, 인증, RBAC, 메뉴 경로와 권한 키는 유지하며 UI 노출을 보안 통제로 사용하지 않는다.

## 현재 문제

- 대시보드가 샘플 API 연결 상태만 표시하고 실제 운영 지표가 없다.
- 데스크톱 상단 헤더, 현재 위치, 빠른 작업과 계정 메뉴가 없다.
- 생성 폼이 목록 위에 항상 펼쳐져 주요 조회 작업을 방해한다.
- 사용자 목록에 검색, 필터, 정렬, 페이지네이션과 열 설정이 없다.
- 권한 화면은 역할 카드 반복 구조라 역할이 늘어날수록 탐색과 비교가 어렵다.
- 메뉴 계층과 파일 업로드 진행 상태가 충분히 시각화되지 않는다.
- 테마 선택기가 주요 업무보다 눈에 띄며 Raleway 중심 타이포그래피는 한글 UI에 어색하다.
- 기능 페이지가 큰 단일 컴포넌트로 구성되어 화면 조합과 서버 상태 로직의 경계가 약하다.

## UI 원칙

- 본문과 데이터에는 한글 가독성이 높은 UI 글꼴을 사용하고 영문 장식용 폰트를 강제하지 않는다.
- 업무 화면은 중립적인 표면과 높은 정보 밀도를 유지하고 primary 색은 선택과 주요 작업에 제한한다.
- 공통 상태는 `Alert`, `Empty`, `Skeleton`, `Spinner`, `Progress`로 표현한다.
- 생성·수정은 `Sheet` 또는 `Dialog`, 파괴적 확인은 `AlertDialog`, 행 작업은 `DropdownMenu`를 사용한다.
- 폼은 `FieldGroup`, `Field`, `FieldSet`을 사용하고 색상은 의미 기반 CSS 토큰만 사용한다.
- `src/components/ui`는 공식 primitive와 최소 호환 보정만 포함하고 업무 조합은 feature의 `component`에 둔다.
- 서버 상태는 TanStack Query hook, feature 전체 UI 상태만 Provider, 지역 UI 상태는 컴포넌트에 둔다.

## 단계별 실행

### 1. 디자인 기반과 관리자 셸

- [x] Noto Sans KR 중심 한글 타이포그래피 적용
- [x] 라이트·다크 배경과 카드 표면 대비 보정
- [x] 테마 3분할 버튼을 아이콘 `DropdownMenu`로 축소
- [x] 데스크톱·모바일 공통 `AppHeader` 추가
- [x] Breadcrumb, Sidebar trigger, 테마, 계정 메뉴 조합
- [x] 사이드바 하단 로그아웃 버튼을 계정 메뉴로 통합
- [x] 공통 loading/empty/error 상태 분리
- [ ] forbidden 상태와 trace ID 표현 통합

### 2. 사용자 관리

- [ ] 검색, 역할·상태 필터, 정렬, 초기화 Toolbar
- [ ] URL query parameter 기반 조회 상태 유지
- [ ] 서버 페이지네이션과 `Pagination` 적용
- [ ] 사용자 생성·수정 `Sheet` 전환
- [ ] 행별 `DropdownMenu`와 상태 변경 `AlertDialog`
- [ ] 데스크톱 Data Table과 모바일 사용자 요약 표현
- [ ] 선택 기반 일괄 작업은 서버 API가 준비된 뒤 적용

### 3. 역할과 권한

- [ ] 역할 탐색과 권한 편집을 분리한 master-detail 화면
- [ ] 역할 검색, 시스템·커스텀 역할 구분
- [ ] namespace별 권한 그룹과 그룹 전체 선택
- [ ] 변경 개수·변경 내용 요약 및 미저장 이동 경고
- [ ] 역할 생성 Sheet와 시스템 역할 수정 제한 설명

### 4. 메뉴 관리

- [ ] 메뉴 트리와 선택 메뉴 상세 편집 분리
- [ ] 부모·자식, 활성 상태, 경로와 권한 키 시각화
- [ ] 아이콘 선택기, 메뉴 검색과 권한 기반 미리보기
- [ ] 안전한 위·아래 순서 이동부터 제공
- [ ] 삭제 시 하위 메뉴 영향 확인

### 5. 파일 관리

- [x] 드래그앤드롭 업로드와 파일 제한 안내
- [x] 선택 파일 정보, 진행률, 성공·실패와 재시도
- [ ] 이름·유형·날짜 검색 및 정렬
- [ ] 파일 상세 Sheet, 작업 Dropdown과 삭제 AlertDialog
- [ ] 지원 형식에 한해 이미지 미리보기

### 6. 운영 대시보드

- [ ] 사용자·역할·파일·저장 용량 지표 카드
- [ ] 사용자 상태와 최근 등록 추이 Chart
- [ ] 최근 활동과 시스템 상태
- [ ] 가짜 데이터 없이 집계 API가 제공하는 항목만 노출
- [ ] `/api/v1/dashboard/*` 집계 API는 별도 backend vertical slice로 구현

### 7. 생산성 기능

- [ ] `Ctrl/Cmd + K` 메뉴 검색과 빠른 이동
- [ ] 최근 방문 메뉴
- [ ] Audit Log 조회
- [ ] 저장된 필터, 테이블 열 표시 설정
- [ ] 서버 지원 범위의 CSV/Excel 내보내기와 일괄 작업

## 기능 디렉터리 기준

```text
features/<feature>/
├─ <Feature>Page.tsx       # 페이지 레이아웃과 컴포넌트 조립
├─ api/                    # 기능 API 계약
├─ provider/               # feature 전체에 공유되는 UI 상태
├─ hook/                   # query, mutation, 파생 상태
└─ component/              # 폼, 테이블, 필터, 작업 메뉴와 dialog/sheet
```

## 작업 단위

1. `style: 관리자 테마와 타이포그래피 개선`
2. `feat: 데스크톱 헤더와 계정 메뉴 추가`
3. `refactor: 공통 페이지와 상태 컴포넌트 분리`
4. `feat: 사용자 관리 데이터 테이블 개선`
5. `feat: 역할 권한 편집 화면 개선`
6. `feat: 메뉴 계층 관리 화면 개선`
7. `feat: 파일 업로드와 목록 UX 개선`
8. `feat: 운영 대시보드 지표 추가`
9. `test: 관리자 핵심 UI 회귀 테스트 보강`
10. `docs: 관리자 UI 구성 규칙 갱신`

## 완료 기준

- 375px, 768px, 1440px에서 핵심 작업을 가로 스크롤 없이 수행한다.
- 라이트·다크 모두 텍스트, 상태, 선택과 포커스 대비가 명확하다.
- 로딩, 빈 상태, 오류, 권한 없음과 재시도 흐름이 일관된다.
- 키보드만으로 Sidebar, Dropdown, Dialog, Sheet와 테이블 작업이 가능하다.
- 기존 서버 RBAC와 403 처리, 화면 경로와 권한 키를 보존한다.
- `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`, `npm run test:e2e`를 통과한다.

## 제외 범위

- 프론트 전용 가짜 알림과 가짜 운영 차트
- UI 숨김으로 서버 권한 검사를 대체하는 구현
- 저장 계약이 없는 메뉴 드래그앤드롭
- PWA, 오프라인, 앱스토어 패키징
- 검증 없이 전체 Admin Framework를 도입하거나 외부 템플릿을 복사하는 작업
