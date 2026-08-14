# RBAC 단계적 적용

현재 RBAC 기반은 사용자당 대표 역할 하나와 코드 기반 권한 catalog를 사용한다. JWT에는 `role`과 `permissions` claim을 전달하고 서버는 `resource:action` 권한으로 API 접근을 강제한다.

| 역할 | 사용자 목록 조회 | 일반 인증 API |
|---|---:|---:|
| `SUPER_ADMIN` | 허용 | 허용 |
| `ADMIN` | 허용 | 허용 |
| `VIEWER` | 거부 | 허용 |

- 기존 사용자와 bootstrap 관리자는 `SUPER_ADMIN`으로 migration된다.
- access token 발급과 rotation 모두 DB의 현재 역할을 claim에 포함한다.
- UI에서 메뉴를 숨기는 것은 인가 수단이 아니며 서버가 최종 판정한다.
- 역할 CRUD와 다중 역할·권한 catalog는 후속 vertical slice에서 별도 테이블로 확장한다.
- 현재 권한 원본은 `roles`, `role_permissions` 테이블이며 `GET /api/v1/permissions`에서 역할별 매핑을 조회할 수 있다. 로그인과 refresh token 회전 시 최신 DB 권한이 access token에 반영된다.
- 역할 변경 시 기존 token 무효화 또는 짧은 TTL 내 반영 정책을 역할 관리 slice에서 확정한다.
