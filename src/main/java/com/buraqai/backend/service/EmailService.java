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


    /**
     * Sends an HTML email notification when a ticket's status is changed.
     * This is a non-critical operation — if email sending fails,
     * the error is logged but the exception is never propagated.
     *
     * @param recipientEmail The employee's email address
     * @param ticketId       The ID of the ticket
     * @param previousStatus The status before the change
     * @param newStatus      The status after the change
     */
    public void sendTicketStatusUpdateEmail(String recipientEmail, Long ticketId,
                                            String previousStatus, String newStatus) {
        if (!mailEnabled) {
            logger.info("Email sending is disabled (app.mail.enabled=false). " +
                            "Skipping ticket status update email | ticketId={} | recipient={} | previousStatus={} | newStatus={}",
                    ticketId, recipientEmail, previousStatus, newStatus);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("ticketId", ticketId);
            context.setVariable("previousStatus", previousStatus);
            context.setVariable("newStatus", newStatus);

            String htmlContent = templateEngine.process("email/ticket-status-update", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(recipientEmail);
            helper.setSubject("Your Ticket #" + ticketId + " Status Has Been Updated");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            logger.info("Ticket status update email sent successfully | ticketId={} | recipient={} | previousStatus={} | newStatus={}",
                    ticketId, recipientEmail, previousStatus, newStatus);

        } catch (MessagingException e) {
            logger.error("Failed to send ticket status update email | ticketId={} | recipient={} | error={}",
                    ticketId, recipientEmail, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending ticket status update email | ticketId={} | recipient={} | error={}",
                    ticketId, recipientEmail, e.getMessage(), e);
        }
    }


    /**
     * Sends an HTML email notification when a support agent responds to a ticket.
     * Only the first 200 characters of the response are included as a preview.
     * This is a non-critical operation — if email sending fails,
     * the error is logged but the exception is never propagated.
     *
     * @param recipientEmail  The employee's email address
     * @param ticketId        The ID of the ticket
     * @param responseText    The full response text from the agent
     */
    public void sendNewResponseEmail(String recipientEmail, Long ticketId, String responseText) {
        if (!mailEnabled) {
            logger.info("Email sending is disabled (app.mail.enabled=false). " +
                    "Skipping new response email | ticketId={} | recipient={}", ticketId, recipientEmail);
            return;
        }

        try {
            // Truncate response to 200 characters for the email preview
            String responsePreview = responseText != null && responseText.length() > 200
                    ? responseText.substring(0, 200) + "..."
                    : responseText;

            Context context = new Context();
            context.setVariable("ticketId", ticketId);
            context.setVariable("responsePreview", responsePreview);

            String htmlContent = templateEngine.process("email/new-response", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(recipientEmail);
            helper.setSubject("New Response on Your Ticket #" + ticketId);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            logger.info("New response email sent successfully | ticketId={} | recipient={} | previewLength={}",
                    ticketId, recipientEmail, responsePreview != null ? responsePreview.length() : 0);

        } catch (MessagingException e) {
            logger.error("Failed to send new response email | ticketId={} | recipient={} | error={}",
                    ticketId, recipientEmail, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending new response email | ticketId={} | recipient={} | error={}",
                    ticketId, recipientEmail, e.getMessage(), e);
        }
    }

    /**
     * Sends an HTML email notification to a support agent when a ticket
     * is assigned to them.
     * This is a non-critical operation — if email sending fails,
     * the error is logged but the exception is never propagated.
     *
     * @param agentEmail  The support agent's email address
     * @param ticketId    The ID of the assigned ticket
     * @param ticketTitle The title/subject of the ticket
     */
    public void sendTicketAssignedEmail(String agentEmail, Long ticketId, String ticketTitle) {
        if (!mailEnabled) {
            logger.info("Email sending is disabled (app.mail.enabled=false). " +
                            "Skipping ticket assigned email | ticketId={} | agent={}",
                    ticketId, agentEmail);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("ticketId", ticketId);
            context.setVariable("ticketTitle", ticketTitle);

            String htmlContent = templateEngine.process("email/ticket-assigned-agent", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(agentEmail);
            helper.setSubject("New Ticket Assigned to You — #" + ticketId);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            logger.info("Ticket assigned email sent successfully | ticketId={} | agent={}",
                    ticketId, agentEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send ticket assigned email | ticketId={} | agent={} | error={}",
                    ticketId, agentEmail, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending ticket assigned email | ticketId={} | agent={} | error={}",
                    ticketId, agentEmail, e.getMessage(), e);
        }
    }


    /**
     * Sends an HTML email notification to the employee (ticket owner)
     * when their ticket is assigned to a support agent and is now in progress.
     * This is a non-critical operation — if email sending fails,
     * the error is logged but the exception is never propagated.
     *
     * @param employeeEmail The employee's email address (ticket owner)
     * @param ticketId      The ID of the ticket
     */
    public void sendTicketInProgressEmail(String employeeEmail, Long ticketId) {
        if (!mailEnabled) {
            logger.info("Email sending is disabled (app.mail.enabled=false). " +
                            "Skipping ticket in progress email | ticketId={} | employee={}",
                    ticketId, employeeEmail);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("ticketId", ticketId);

            String htmlContent = templateEngine.process("email/ticket-in-progress", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(employeeEmail);
            helper.setSubject("Your Ticket #" + ticketId + " is Now In Progress");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            logger.info("Ticket in progress email sent successfully | ticketId={} | employee={}",
                    ticketId, employeeEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send ticket in progress email | ticketId={} | employee={} | error={}",
                    ticketId, employeeEmail, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending ticket in progress email | ticketId={} | employee={} | error={}",
                    ticketId, employeeEmail, e.getMessage(), e);
        }
    }
}