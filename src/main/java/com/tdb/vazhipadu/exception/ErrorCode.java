package com.tdb.vazhipadu.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    DUPLICATE_OFFERING(HttpStatus.CONFLICT),
    DATA_CONFLICT(HttpStatus.CONFLICT),
    DAILY_LIMIT_EXCEEDED(HttpStatus.CONFLICT),
    TRY_AGAIN(HttpStatus.SERVICE_UNAVAILABLE),
    OFFERING_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY),
    UNKNOWN_OFFERING(HttpStatus.UNPROCESSABLE_ENTITY),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
