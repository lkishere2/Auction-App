package com.auction.app.domains.auth.email;

import jakarta.mail.MessagingException;

public interface EmailService {

    void sendVerificationMail(String to, String subject, String text) throws MessagingException;

}
