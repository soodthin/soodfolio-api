package com.soodfolio.api.service;

public interface EmailService {

    /**
     * Sends password reset email containing the secure reset link.
     *
     * @param toEmail       Recipient email address
     * @param recipientName Recipient full name
     * @param resetLink     Full reset link containing the raw token
     */
    void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink);
}
