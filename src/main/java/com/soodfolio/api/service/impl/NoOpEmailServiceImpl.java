package com.soodfolio.api.service.impl;

import com.soodfolio.api.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "noop", matchIfMissing = true)
public class NoOpEmailServiceImpl implements EmailService {

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        log.debug("NoOpEmailService: skipping email delivery for password reset");
    }
}
