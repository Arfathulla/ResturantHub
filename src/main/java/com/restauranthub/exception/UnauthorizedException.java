package com.restauranthub.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN_ACCESS", message);
    }
}
