# common-excel

신뢰할 수 없는 Excel 셀을 안전하게 처리하고, 업무 모델과 Apache POI 타입을 분리하는 XLSX 표 입출력 기반 모듈입니다.

- `ExcelCellSafety.safeText`: `=`, `+`, `-`, `@`로 시작하는 export 문자열을 작은따옴표로 중화
- `XlsxTableReader`: 기대 header와 최대 행 수를 적용하여 첫 sheet를 `XlsxRow` 목록으로 변환
- `XlsxTableWriter`: header와 typed cell을 streaming workbook으로 작성하며 text 셀을 자동 중화
- `XlsxReadException.Reason`: header 불일치, 행 제한, 수식 셀, 손상된 workbook을 consumer가 자체 오류 계약으로 변환할 수 있는 안정된 실패 분류

업무별 컬럼, 파일 크기 제한, 오류 문구, 행 값 검증과 command 변환은 consumer에 남겨야 합니다. 공용 API와 compile dependency에는 POI 타입이 노출되지 않으며 POI는 runtime 구현 세부사항입니다.
