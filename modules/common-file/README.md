# common-file

Local 또는 S3 호환 object storage를 같은 `FileStorage` 계약으로 사용하는 모듈입니다.

- `app.file-storage.type`: `local`(기본값) 또는 `s3`
- `app.file-storage.root`: Local root, 기본값 `./data/files`
- `app.file-storage.s3.*`: endpoint, region, bucket, access/secret key, path-style 설정

auto-configuration은 consumer가 `FileStorage` bean을 제공하면 기본 adapter를 등록하지 않습니다.
업로드 크기·MIME, metadata, 소유권과 삭제 보상은 consumer 정책이며 이 모듈에 포함하지 않습니다.
