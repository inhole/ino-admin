# 파일 API

모든 파일 API는 JWT 인증과 파일 권한이 필요하며, 다운로드는 업로드한 소유자에게만 허용한다.

- `POST /api/v1/files`: `multipart/form-data`의 `file` part를 업로드한다. `file:write` 권한 필요.
- `GET /api/v1/files`: 본인이 업로드한 파일을 검색·정렬하여 조회한다. `file:read` 권한 필요.
- `GET /api/v1/files/{fileId}/content`: 첨부 응답으로 다운로드한다. `file:read` 권한 필요.
- `DELETE /api/v1/files/{fileId}`: 본인 파일의 object와 metadata를 삭제한다. `file:write` 권한 필요.

기본 최대 크기는 10MB이며 PDF, PNG, JPEG, 일반 텍스트만 허용한다. 서버는 확장자, 선언된 MIME, 파일 시그니처를 함께 검증하고 실제 저장명에는 임의 UUID를 사용한다. 저장 루트와 최대 크기는 각각 `APP_FILE_STORAGE_ROOT`, `APP_FILE_MAX_SIZE`로 변경할 수 있다.

`APP_FILE_MAX_SIZE`는 도메인 검증과 Servlet multipart의 단일 파일 제한에 함께 적용된다. multipart envelope를 위해 전체 요청에는 1MB 여유를 두며, 한도를 초과하면 `413 FILE_TOO_LARGE` 오류를 반환한다.

## 목록 조회 조건

`GET /api/v1/files`는 다음 query parameter를 지원한다. 모든 조건은 현재 인증 사용자가 소유하고 `READY` 상태인 파일 안에서만 적용된다.

| parameter | 기본값 | 설명 |
|---|---:|---|
| `page` | `0` | 0부터 시작하는 페이지 번호 |
| `size` | `20` | 페이지 크기, 1~100 |
| `name` | - | 원본 파일명 부분 일치, 대소문자 구분 없음 |
| `contentType` | - | `application/pdf`, `image/png`, `image/jpeg`, `text/plain` 중 하나 |
| `createdFrom` | - | 포함되는 업로드 시각, ISO 8601 UTC instant |
| `createdTo` | - | 포함되지 않는 업로드 종료 시각, ISO 8601 UTC instant |
| `sort` | `createdAt` | `createdAt`, `originalName`, `size` 중 하나 |
| `direction` | `desc` | `asc` 또는 `desc` |

웹의 종료일 필터는 사용자의 로컬 날짜 다음 날 0시를 UTC instant로 변환해 `createdTo`에 전달하므로 선택한 종료일 전체를 포함한다. 지원하지 않는 MIME, 정렬 필드, 방향은 `400 VALIDATION_ERROR`로 거부한다.

삭제 요청은 metadata를 먼저 `DELETING`으로 커밋한 뒤 object를 제거한다. object 저장소 장애가 발생하면 목록과 다운로드에서 즉시 제외하고, 기본 1분 간격 cleanup job이 최대 100건씩 재시도한다. 재시도 간격은 `app.file-storage.cleanup-delay`로 조정할 수 있다.

`APP_FILE_STORAGE_TYPE=s3`로 바꾸면 동일 API가 S3/MinIO adapter를 사용한다. bucket은 배포 전에 생성해야 하며 MinIO에서는 endpoint, access key, secret key와 path-style access를 설정한다. AWS에서는 정적 credential을 비워 SDK 기본 credential chain을 사용한다.
