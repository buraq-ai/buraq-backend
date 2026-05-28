package com.buraqai.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.enabled}")
    private boolean mailEnabled;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends an HTML email notification when a support ticket is created.
     * This is a non-critical operation — if email sending fails,
     * the error is logged but the exception is never propagated.
     *
     * @param recipientEmail The employee's email address
     * @param ticketId       The ID of the created ticket
     * @param ticketTitle    The title/subject of the ticket
     */
    public void sendTicketCreatedEmail(String recipientEmail, Long ticketId, String ticketTitle) {
        // Check if email sending is enabled — if not, log and return silently
        if (!mailEnabled) {
            logger.info("Email sending is disabled (app.mail.enabled=false). Skipping ticket creation email | " +
                    "ticketId={} | recipient={}", ticketId, recipientEmail);
            return;
        }

        try {
            // Build Thymeleaf context with template variables
            Context context = new Context();
            context.setVariable("ticketId", ticketId);
            context.setVariable("ticketTitle", ticketTitle);

            // Process the HTML template
            String htmlContent = templateEngine.process("email/ticket-created", context);

            // Create and configure the MIME message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(recipientEmail);
            helper.setSubject("Your Support Ticket #" + ticketId + " Has Been Created");
            helper.setText(htmlContent, true); // true = HTML content

            // Send the email
            mailSender.send(message);

            logger.info("Ticket creation email sent successfully | ticketId={} | recipient={}",
                    ticketId, recipientEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send ticket creation email | ticketId={} | recipient={} | error={}",
                    ticketId, recipientEmail, e.getMessage(), e);
            // Exception is caught and logged — never re-thrown to avoid breaking the application
        } catch (Exception e) {
            logger.error("Unexpected error while sending ticket creation email | ticketId={} | recipient={} | error={}",
                    ticketId, recipientEmail, e.getMessage(), e);
            // Catch-all for any other unexpected exceptions (e.g., template not found)
        }
    }
}