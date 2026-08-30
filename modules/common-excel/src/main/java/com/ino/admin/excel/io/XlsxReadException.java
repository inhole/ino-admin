package com.ino.admin.excel.io;

public final class XlsxReadException extends RuntimeException {
    private final Reason reason;
    private final Integer rowNumber;

    public XlsxReadException(Reason reason) {
        this(reason, null, null);
    }

    public XlsxReadException(Reason reason, Integer rowNumber) {
        this(reason, rowNumber, null);
    }

    public XlsxReadException(Reason reason, Throwable cause) {
        this(reason, null, cause);
    }

    private XlsxReadException(Reason reason, Integer rowNumber, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
        this.rowNumber = rowNumber;
    }

    public Reason reason() { return reason; }
    public Integer rowNumber() { return rowNumber; }

    public enum Reason {
        MISSING_SHEET,
        HEADER_MISMATCH,
        ROW_LIMIT_EXCEEDED,
        FORMULA_CELL,
        INVALID_WORKBOOK
    }
}
