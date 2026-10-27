package com.tdb.vazhipadu.exception;

public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public static ApiException notFound(String resource, Object id) {
        return new ApiException(ErrorCode.NOT_FOUND, "%s %s not found".formatted(resource, id));
    }

    public ErrorCode code() {
        return code;
    }
}
