# 프론트엔드 디렉터리 구조

`admin-web`은 기능 경계를 우선하는 구조를 사용한다.

```text
src/
├─ app/                 # route와 애플리케이션 조립
├─ features/            # 사용자 기능 단위 UI와 상태
│  ├─ auth/
│  │  ├─ model/         # 인증 상태, context, hook
│  │  └─ ui/            # 로그인과 보호 route
│  └─ dashboard/        # 대시보드 화면
├─ components/ui/       # shadcn/ui 기반 공통 표현 컴포넌트
├─ api/                 # 서버 API 계약과 transport
├─ lib/                 # 업무 규칙이 없는 작은 기반 유틸리티
└─ test/                # 전역 테스트 설정
```

## 의존성 규칙

- `app`은 feature를 조립할 수 있다.
- feature는 `api`, `components/ui`, `lib`를 사용할 수 있다.
- feature 간 참조는 공개된 hook이나 컴포넌트로 제한하고 순환 참조를 만들지 않는다.
- `components/ui`는 feature와 `app`을 참조하지 않는다.
- HTTP 호출은 React 컴포넌트에 직접 작성하지 않고 `src/api`에 둔다.
- 서버의 인증·인가 판정을 UI의 route 또는 버튼 숨김으로 대체하지 않는다.

새 화면은 기능 이름 아래에 추가하고, 두 개 이상의 feature에서 실제로 사용하는 표현 컴포넌트만 `components` 또는 `lib`로 올린다.
