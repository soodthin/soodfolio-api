package com.soodfolio.api.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends BaseApiException {

    public RateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, cause);
    }
}
