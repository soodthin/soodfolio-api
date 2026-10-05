package com.soodfolio.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseApiException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, HttpStatus.UNAUTHORIZED, cause);
    }
}
