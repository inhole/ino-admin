# common-web

Spring MVC 애플리케이션에서 공통 오류 응답과 요청 trace ID를 재사용하는 모듈입니다.

- `ApiError`, `ApiErrorFactory`: consumer가 전달한 문자열 code/message와 주입한 `Clock`으로 오류 응답 생성
- `TraceIdFilter`: `X-Trace-Id`를 응답 헤더와 MDC에 연결
- auto-configuration: factory와 filter를 consumer bean이 없을 때 등록

consumer는 `Clock` bean을 제공해야 합니다. 업무별 controller, DTO, 예외 매핑 정책은 포함하지 않습니다.
