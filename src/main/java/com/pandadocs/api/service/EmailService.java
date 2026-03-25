package com.pandadocs.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${mail.from.email:no-reply@example.com}")
    private String fromEmail;

    @Value("${spring.mail.host:smtp.sendgrid.net}")
    private String mailHost;

    @Value("${spring.mail.port:2525}")
    private String mailPort;

    @Value("${spring.mail.username:apikey}")
    private String mailUsername;

    @PostConstruct
    public void logMailConfiguration() {
        logger.info("=== EMAIL SERVICE CONFIGURATION ===");
        logger.info("Using SMTP");
        logger.info("Mail Host: {}", mailHost);
        logger.info("Mail Port: {}", mailPort);
        logger.info("Mail Username: {}", mailUsername);
        logger.info("Mail From Email: {}", fromEmail);

        String mailPassword = System.getenv("MAIL_PASSWORD");
        if (mailPassword != null) {
            logger.info("Mail Password Set: YES");
        } else {
            logger.info("Mail Password Set: NO");
        }

        if (mailSender instanceof JavaMailSenderImpl) {
            JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;
            logger.info("JavaMailSender Properties: {}", mailSenderImpl.getJavaMailProperties());
        }
        logger.info("===================================");
    }

    @Async
    public void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        try {
            logger.info("Attempting to send email to: {}, subject: {}", to, subject);
            logger.info("Using mail host: {}, port: {}, username: {}", mailHost, mailPort, mailUsername);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            logger.info("Email message prepared successfully, sending...");
            mailSender.send(mimeMessage);
            logger.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            logger.error("Error type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            throw e;
        }
    }
}
