# 초기 관리자 Bootstrap

## 목적

빈 데이터베이스에 최초 로그인용 관리자 계정 하나를 안전하게 생성한다. 계정이나 비밀번호는 Flyway migration과 저장소에 포함하지 않는다.

## 실행 전 조건

- PostgreSQL이 실행 중이고 V2 migration을 적용할 수 있어야 한다.
- 비밀번호는 12~128자이며 대문자·소문자·숫자·특수문자를 모두 포함해야 한다.
- 환경 변수 값이 shell history나 운영 로그에 남지 않도록 배포 환경의 secret 주입 기능을 사용한다.

## UTF-8 입력

저장소 루트의 선택적 `.env` 파일을 사용할 때는 파일을 반드시 **UTF-8**로 저장한다. 터미널의 콘솔 코드 페이지에 의존해 파일을 작성하거나 변환하지 않는다. 서버는 `.env`를 UTF-8로 읽고, OS 또는 배포 환경이 주입한 환경 변수는 `.env`보다 우선한다.

PowerShell에서는 다음처럼 환경 변수를 직접 설정할 수 있다. 이 방법도 값을 전달하는 실행 환경이 UTF-8을 지원하도록 구성해야 하며, 운영 환경에서는 명령 이력 대신 secret 주입을 사용한다.

## 실행

```powershell
$env:APP_BOOTSTRAP_ADMIN_ENABLED='true'
$env:APP_BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD='<강한 임시 비밀번호>'
$env:APP_BOOTSTRAP_ADMIN_DISPLAY_NAME='시스템 관리자'
./gradlew.bat :apps:admin-server:bootRun
```

계정이 생성되면 서버는 이메일이나 비밀번호를 포함하지 않는 성공 메시지만 기록한다. 같은 이메일이 이미 있으면 성공적으로 건너뛰므로 재실행할 수 있다.

## 실행 후 조치

1. 서버 시작 로그에서 생성 완료 또는 기존 계정 건너뛰기 메시지를 확인한다.
2. `APP_BOOTSTRAP_ADMIN_ENABLED=false`로 되돌린다.
3. `APP_BOOTSTRAP_ADMIN_PASSWORD` secret을 제거하거나 폐기한다.
4. Phase 2 로그인 기능이 준비되면 최초 로그인 직후 임시 비밀번호를 변경한다.

## 실패 처리

- 이메일, 표시 이름, 비밀번호 정책이 잘못되면 서버 시작이 실패하고 설정 항목을 설명하는 오류가 출력된다.
- DB 연결 또는 migration이 실패하면 계정은 생성되지 않는다. DB 상태를 복구한 뒤 같은 명령을 다시 실행한다.
- 비밀번호 원문과 hash를 로그 또는 장애 보고서에 첨부하지 않는다.
- 이미 깨져 저장된 표시 이름은 자동으로 변환하지 않는다. 사용자 관리 기능에서 정확한 이름으로 수정하거나, 운영자가 대상과 값을 검토한 뒤 바인드 파라미터를 사용하는 SQL update로 수정한다.
