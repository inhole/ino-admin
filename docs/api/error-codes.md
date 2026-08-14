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
| `USER_NOT_FOUND` | 400 | 상태를 변경할 사용자를 찾을 수 없다. |
| `UNAUTHORIZED` | 401 | bearer token이 없거나 서명·만료·issuer·audience 검증에 실패했다. |
| `FORBIDDEN` | 403 | 인증되었지만 요청을 수행할 역할 또는 권한이 없다. |
| `INTERNAL_ERROR` | 500 | An unexpected server error occurred. |

Clients should branch on `code`, not localized `message` text. Quote the `traceId` when reporting an incident.
