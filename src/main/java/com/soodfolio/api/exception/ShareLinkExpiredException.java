package com.soodfolio.api.exception;

import org.springframework.http.HttpStatus;

public class ShareLinkExpiredException extends BaseApiException {

    public ShareLinkExpiredException(String message) {
        super(message, HttpStatus.GONE);
    }
}
