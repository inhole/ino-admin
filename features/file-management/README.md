# File Management Feature

파일 메타데이터와 소유권 정책, 저장소 port 및 검증된 Local adapter를 제공한다. HTTP multipart 계약과 다운로드 응답 헤더는 `admin-server`가 담당한다.

기본 허용 형식은 PDF, PNG, JPEG, 일반 텍스트이며 기본 최대 크기는 10MB다. 저장 경로는 `app.file-storage.root`, 크기는 `app.file-storage.max-size`로 설정한다.
