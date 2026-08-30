# common-excel

신뢰할 수 없는 Excel 셀을 안전하게 처리하기 위한 최소 기반 모듈입니다.

- `ExcelCellSafety.safeText`: `=`, `+`, `-`, `@`로 시작하는 export 문자열을 작은따옴표로 중화
- `ExcelCellSafety.rejectFormulas`: 지정한 열 범위의 formula cell을 import 전에 거부

업무별 컬럼, header, 행·파일 제한, 오류 문구와 command 변환은 consumer에 남겨야 합니다.
