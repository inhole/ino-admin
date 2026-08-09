# API error codes

All errors include `code`, `message`, `fieldErrors`, `traceId`, and an ISO-8601 UTC `timestamp`.

| Code | HTTP status | Meaning |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | One or more request fields are invalid. |
| `INTERNAL_ERROR` | 500 | An unexpected server error occurred. |

Clients should branch on `code`, not localized `message` text. Quote the `traceId` when reporting an incident.
