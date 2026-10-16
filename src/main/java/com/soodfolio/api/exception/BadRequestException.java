package com.soodfolio.api.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseApiException {
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
