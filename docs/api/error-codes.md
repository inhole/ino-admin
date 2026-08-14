# API error codes

All errors include `code`, `message`, `fieldErrors`, `traceId`, and an ISO-8601 UTC `timestamp`.

| Code | HTTP status | Meaning |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | One or more request fields are invalid. |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 올바르지 않거나 계정을 사용할 수 없다. |
| `UNAUTHORIZED` | 401 | bearer token이 없거나 서명·만료·issuer·audience 검증에 실패했다. |
| `INTERNAL_ERROR` | 500 | An unexpected server error occurred. |

Clients should branch on `code`, not localized `message` text. Quote the `traceId` when reporting an incident.
