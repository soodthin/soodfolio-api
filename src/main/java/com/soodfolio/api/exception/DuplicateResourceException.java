package com.soodfolio.api.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseApiException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, cause);
    }
}
