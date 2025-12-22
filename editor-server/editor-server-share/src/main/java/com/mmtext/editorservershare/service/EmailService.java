package com.mmtext.editorservershare.service;


import com.mmtext.editorservershare.enums.PermissionLevel;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Value("${app.name:Editor App}")
    private String appName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendDocumentSharedEmail(
            String toEmail,
            String toName,
            String documentId,
            String documentTitle,
            PermissionLevel permissionLevel,
            String sharerName,
            String customMessage) {

        String documentUrl = frontendUrl + "/editor/" + documentId;

        String htmlContent = buildDocumentSharedEmail(
                toName, documentTitle, permissionLevel, sharerName, customMessage, documentUrl
        );

        sendEmail(toEmail,
                String.format("%s shared \"%s\" with you", sharerName, documentTitle),
                htmlContent);

        log.info("Document shared email sent to {}", toEmail);
    }

    @Async
    public void sendAccessRequestEmail(
            String ownerEmail,
            String ownerName,
            String documentId,
            String documentTitle,
            String requesterName,
            String requesterEmail,
            PermissionLevel requestedPermission,
            String message,
            UUID requestId,
            String approvalToken) {

        String htmlContent = buildAccessRequestEmail(
                ownerName, documentTitle, requesterName, requesterEmail,
                requestedPermission, message, requestId.toString(), approvalToken, approvalToken
        );

        sendEmail(ownerEmail,
                String.format("%s requested access to \"%s\"", requesterName, documentTitle),
                htmlContent);

        log.info("Access request email sent to {}", ownerEmail);
    }

    @Async
    public void sendAccessApprovedEmail(
            String toEmail,
            String toName,
            String documentId,
            String documentTitle,
            PermissionLevel grantedPermission) {

        String documentUrl = frontendUrl + "/editor/" + documentId;

        String htmlContent = buildAccessApprovedEmail(
                toName, documentTitle, grantedPermission, documentUrl
        );

        sendEmail(toEmail,
                String.format("Access approved: \"%s\"", documentTitle),
                htmlContent);

        log.info("Access approved email sent to {}", toEmail);
    }

    @Async
    public void sendAccessRejectedEmail(
            String toEmail,
            String toName,
            String documentTitle) {

        String htmlContent = buildAccessRejectedEmail(toName, documentTitle);

        sendEmail(toEmail,
                String.format("Access request update: \"%s\"", documentTitle),
                htmlContent);

        log.info("Access rejected email sent to {}", toEmail);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    // HTML Email Templates (Inline)

    private String buildDocumentSharedEmail(String recipientName, String documentTitle,
                                            PermissionLevel permissionLevel, String sharerName, String customMessage, String documentUrl) {

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .message-box { background: #f8f9fa; border-left: 4px solid #667eea; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .button { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📄 Document Shared</h1>
                    </div>
                    <div class="content">
                        <p>Hi <strong>%s</strong>,</p>
                        <p><strong>%s</strong> shared a document with you:</p>
                        <div class="message-box">
                            <h3>%s</h3>
                            <p>Your access: <strong>%s</strong></p>
                        </div>
                        %s
                        <p style="text-align: center;">
                            <a href="%s" class="button">Open Document</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>© 2024 %s. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                recipientName, sharerName, documentTitle, getPermissionLabel(permissionLevel),
                customMessage != null ? "<p><em>" + customMessage + "</em></p>" : "",
                documentUrl, appName
        );
    }

    private String buildAccessRequestEmail(String ownerName, String documentTitle,
                                           String requesterName, String requesterEmail, PermissionLevel requestedPermission,
                                           String message, String requestId, String approveToken, String rejectToken) {

        String approveUrl = backendUrl + "/api/share/access-requests/" + requestId + "/approve?token=" + approveToken;
        String rejectUrl = backendUrl + "/api/share/access-requests/" + requestId + "/reject?token=" + rejectToken;

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; background: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; }
                    .header { background: linear-gradient(135deg, #2196f3 0%%, #1976d2 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .info-box { background: #e3f2fd; border-left: 4px solid #2196f3; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .button { display: inline-block; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 10px; color: white; font-weight: bold; }
                    .approve { background: #4caf50; }
                    .reject { background: #f44336; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔔 Access Request</h1>
                    </div>
                    <div class="content">
                        <p>Hi <strong>%s</strong>,</p>
                        <div class="info-box">
                            <h3>%s</h3>
                            <p><strong>%s</strong> (%s) requested <strong>%s</strong> access</p>
                            %s
                        </div>
                        <p style="text-align: center;">
                            <a href="%s" class="button approve">✓ Approve</a>
                            <a href="%s" class="button reject">✗ Reject</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Editor App. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                ownerName, documentTitle, requesterName, requesterEmail, getPermissionLabel(requestedPermission),
                message != null ? "<p><em>" + message + "</em></p>" : "",
                approveUrl, rejectUrl
        );
    }

    private String buildAccessApprovedEmail(String recipientName, String documentTitle,
                                            PermissionLevel grantedPermission, String documentUrl) {

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; }
                    .header { background: linear-gradient(135deg, #4caf50 0%%, #45a049 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; text-align: center; }
                    .success { font-size: 48px; color: #4caf50; }
                    .button { display: inline-block; padding: 14px 35px; background: #4caf50; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✓ Access Approved</h1>
                    </div>
                    <div class="content">
                        <div class="success">🎉</div>
                        <h2>Great News!</h2>
                        <p>Hi <strong>%s</strong>,</p>
                        <p>Your access request was approved!</p>
                        <p>Document: <strong>%s</strong></p>
                        <p>Access level: <strong>%s</strong></p>
                        <a href="%s" class="button">Open Document Now</a>
                    </div>
                </div>
            </body>
            </html>
            """,
                recipientName, documentTitle, getPermissionLabel(grantedPermission), documentUrl
        );
    }

    private String buildAccessRejectedEmail(String recipientName, String documentTitle) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; padding: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Access Request Update</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>Your access request to <strong>"%s"</strong> was not approved at this time.</p>
                </div>
            </body>
            </html>
            """,
                recipientName, documentTitle
        );
    }

    private String getPermissionLabel(PermissionLevel level) {
        return switch (level) {
            case OWNER -> "Owner";
            case EDITOR -> "Can edit";
            case VIEWER -> "Can view";
        };
    }
}