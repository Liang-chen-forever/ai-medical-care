package com.liang.medical.common;

import org.springframework.http.HttpStatus;

/**
 * 可预期的领域错误，保留 HTTP 状态和面向客户端的错误信息。
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final int code;

    public BusinessException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }
}
