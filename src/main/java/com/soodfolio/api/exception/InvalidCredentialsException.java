package com.soodfolio.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BaseApiException {

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, HttpStatus.UNAUTHORIZED, cause);
    }
}
