package com.eec.bookhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender sender;

    @Value("${spring.mail.username}")
    private String from;

    public MailService(JavaMailSender sender) {
        this.sender = sender;
    }

    public void sendPasswordOtp(String to, String otp, int expiryMinutes) {

        SimpleMailMessage m = new SimpleMailMessage();

        m.setFrom(from);
        m.setTo(to);

        m.setSubject("EEC BookHub password reset OTP");

        m.setText(
            "Your EEC BookHub password reset OTP is: " + otp +
            "\n\nIt expires in " + expiryMinutes +
            " minutes." +
            "\n\nIf you did not request this, ignore this email."
        );

        sender.send(m);
    }
}