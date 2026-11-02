package com.soodfolio.api.exception;

import org.springframework.http.HttpStatus;

public class AiServiceUnavailableException extends BaseApiException {

    public AiServiceUnavailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
