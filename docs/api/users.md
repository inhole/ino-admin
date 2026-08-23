# 사용자 API

`GET /api/v1/users`는 JWT 인증과 `user:read` 권한이 필요하다. 서버는 조회 조건과 정렬을 검증한 뒤 사용자 목록 페이지를 반환하며, Entity나 내부 속성은 그대로 노출하지 않는다.

## 목록 조회 조건

| parameter | 기본값 | 허용값/계약 |
|---|---:|---|
| `query` | `""` | 이름 또는 이메일 부분 일치, 대소문자 구분 없음, 최대 320자 |
| `role` | `""` | 정확한 역할 키 일치, 최대 50자 |
| `status` | `""` | 빈 값, `ACTIVE`, `LOCKED`, `DISABLED` |
| `page` | `0` | 0 이상 |
| `size` | `20` | 1~100 |
| `sort` | `createdAt` | `createdAt`, `displayName`, `email`, `role`, `status` |
| `direction` | `desc` | `asc`, `desc` |

정렬은 요청한 필드 뒤에 항상 `id ASC`를 보조 정렬로 추가해 페이지 경계를 안정화한다.

## 응답 계약

응답은 기존 page response 구조를 유지한다.

| field | 설명 |
|---|---|
| `content` | 사용자 요약 목록 |
| `content[].id` | 사용자 식별자 UUID |
| `content[].email` | 이메일 |
| `content[].displayName` | 표시 이름 |
| `content[].status` | 사용자 상태 |
| `content[].role` | 역할 키 |
| `content[].createdAt` | UTC 생성 시각 |
| `page` | 현재 페이지 번호 |
| `size` | 페이지 크기 |
| `totalElements` | 전체 사용자 수 |
| `totalPages` | 전체 페이지 수 |

예시:

```json
{
  "content": [
    {
      "id": "7ef7d4ad-bf95-49e2-85eb-769e54ce5f45",
      "email": "kim@example.com",
      "displayName": "김관리",
      "status": "ACTIVE",
      "role": "ADMIN",
      "createdAt": "2026-08-20T09:30:00Z"
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 21,
  "totalPages": 3
}
```

## 오류 계약

- 지원하지 않는 `status`, `sort`, `direction` 값이나 범위를 벗어난 `page`, `size`는 `400 VALIDATION_ERROR`를 반환한다.
- 인증은 되었지만 `user:read` 권한이 없으면 `403 FORBIDDEN`을 반환한다.
