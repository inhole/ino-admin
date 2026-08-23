# API error codes

All errors include `code`, `message`, `fieldErrors`, `traceId`, and an ISO-8601 UTC `timestamp`.

| Code | HTTP status | Meaning |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | One or more request fields are invalid. |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 올바르지 않거나 계정을 사용할 수 없다. |
| `INVALID_CURRENT_PASSWORD` | 400 | 비밀번호 변경 요청의 현재 비밀번호가 올바르지 않다. |
| `PASSWORD_REUSE_NOT_ALLOWED` | 400 | 새 비밀번호가 현재 비밀번호와 동일하다. |
| `PASSWORD_POLICY_VIOLATION` | 400 | 새 비밀번호가 길이 또는 문자 조합 정책을 충족하지 않는다. |
| `EMAIL_ALREADY_EXISTS` | 400 | 사용자 생성 이메일이 이미 등록되어 있다. |
| `INVALID_USER_ROLE` | 400 | 사용자 생성 시 허용되지 않은 역할을 요청했다. |
| `INVALID_USER_STATUS` | 400 | 활성 또는 비활성 외의 사용자 상태 변경을 요청했다. |
| `SELF_DISABLE_NOT_ALLOWED` | 400 | 관리자가 자기 계정 비활성화를 요청했다. |
| `SELF_ROLE_CHANGE_NOT_ALLOWED` | 400 | 관리자가 자기 계정의 역할 변경을 요청했다. |
| `LAST_SUPER_ADMIN_PROTECTED` | 400 | 마지막 활성 최고 관리자의 비활성화 또는 역할 변경을 요청했다. |
| `USER_NOT_FOUND` | 400 | 상태를 변경할 사용자를 찾을 수 없다. |
| `MENU_ID_ALREADY_EXISTS` | 400 | 이미 존재하는 메뉴 ID를 생성하려 했다. |
| `MENU_NOT_FOUND` | 400 | 변경할 메뉴가 존재하지 않는다. |
| `INVALID_MENU_PARENT` | 400 | 지정한 부모 메뉴가 존재하지 않는다. |
| `MENU_CYCLE` | 400 | 메뉴 부모 관계가 순환한다. |
| `MENU_ORDER_DUPLICATED` | 400 | 동일 부모 아래 정렬 순서가 중복된다. |
| `ROLE_NOT_FOUND` | 400 | 사용자에게 지정된 역할이 catalog에 존재하지 않는다. |
| `SYSTEM_ROLE_PROTECTED` | 400 | 최고 관리자 역할의 권한 변경을 요청했다. |
| `INVALID_PERMISSION` | 400 | catalog에 없는 권한을 역할에 할당하려 했다. |
| `INVALID_ROLE_KEY` | 400 | 커스텀 역할 키 형식이 올바르지 않다. |
| `ROLE_ALREADY_EXISTS` | 400 | 동일한 역할 키가 이미 존재한다. |
| `UNAUTHORIZED` | 401 | bearer token이 없거나 서명·만료·issuer·audience 검증에 실패했다. |
| `FORBIDDEN` | 403 | 인증되었지만 요청을 수행할 역할 또는 권한이 없다. |
| `INVALID_FILE` | 400 | 파일 이름, 크기, 확장자, MIME 또는 실제 내용이 업로드 정책과 맞지 않는다. |
| `FILE_NOT_FOUND` | 404 | 파일이 없거나 요청자가 해당 파일의 소유자가 아니다. |
| `FILE_TOO_LARGE` | 413 | multipart 요청의 파일 크기가 설정된 최대값을 초과했다. |
| `INTERNAL_ERROR` | 500 | An unexpected server error occurred. |

Clients should branch on `code`, not localized `message` text. Quote the `traceId` when reporting an incident.
