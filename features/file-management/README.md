# File Management Feature

파일 메타데이터와 소유권 정책, 저장소 port 및 검증된 Local adapter를 제공한다. HTTP multipart 계약과 다운로드 응답 헤더는 `admin-server`가 담당한다.

기본 허용 형식은 PDF, PNG, JPEG, 일반 텍스트이며 기본 최대 크기는 10MB다. 저장 경로는 `app.file-storage.root`, 크기는 `app.file-storage.max-size`로 설정한다.

`app.file-storage.type=s3`로 설정하면 S3 adapter를 사용한다. MinIO는 endpoint와 path-style access를 지정하고, 운영 AWS에서는 endpoint와 정적 credential을 비워 기본 AWS credential chain을 사용한다. bucket은 사전에 생성되어 있어야 한다.

CI는 MinIO 컨테이너와 실제 S3 client를 사용해 저장, 조회, 삭제 계약을 검증한다. 통합 테스트가 사용할 bucket은 테스트 시작 시 없으면 생성한다.
