# 파일 API

모든 파일 API는 JWT 인증과 파일 권한이 필요하며, 다운로드는 업로드한 소유자에게만 허용한다.

- `POST /api/v1/files`: `multipart/form-data`의 `file` part를 업로드한다. `file:write` 권한 필요.
- `GET /api/v1/files/{fileId}/content`: 첨부 응답으로 다운로드한다. `file:read` 권한 필요.

기본 최대 크기는 10MB이며 PDF, PNG, JPEG, 일반 텍스트만 허용한다. 서버는 확장자, 선언된 MIME, 파일 시그니처를 함께 검증하고 실제 저장명에는 임의 UUID를 사용한다. 저장 루트와 최대 크기는 각각 `APP_FILE_STORAGE_ROOT`, `APP_FILE_MAX_SIZE`로 변경할 수 있다.
