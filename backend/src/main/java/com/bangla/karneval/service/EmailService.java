package com.bangla.karneval.service;

import com.bangla.karneval.model.Registration;
import com.bangla.karneval.util.EmailTemplateUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired private JavaMailSender   mailSender;
    @Autowired private EmailTemplateUtil emailTemplateUtil;

    @Value("${spring.mail.username}") private String fromEmail;

    @Async
    public void sendRegistrationConfirmation(Registration reg) {
        sendHtmlEmail(
            reg.getEmail(),
            "🎉 Registration Confirmed — Bangla Karneval " + reg.getEventYear(),
            emailTemplateUtil.buildRegistrationConfirmationEmail(reg)
        );
    }

    @Async
    public void sendPaymentConfirmation(Registration reg) {
        sendHtmlEmail(
            reg.getEmail(),
            "✅ Payment Confirmed — Bangla Karneval " + reg.getEventYear(),
            emailTemplateUtil.buildPaymentConfirmationEmail(reg)
        );
    }

    @Async
    public void sendPaymentReminder(Registration reg) {
        sendHtmlEmail(
            reg.getEmail(),
            "⏰ Payment Reminder — Bangla Karneval " + reg.getEventYear(),
            emailTemplateUtil.buildPaymentReminderEmail(reg)
        );
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    // ── Contact reply ─────────────────────────────────────────────
    @Async
    public void sendContactReply(String name, String toEmail,
                                 String originalMessage, String replyText) {
        sendHtmlEmail(
                toEmail,
                "📬 Re: Your message to Bangla Karneval",
                emailTemplateUtil.buildContactReplyEmail(name, originalMessage, replyText)
        );
    }

    // ── Registration status with admin note ──────────────────────
    @Async
    public void sendRegistrationStatusEmail(Registration reg, String adminNote) {
        String subject = reg.getPaymentStatus().name().equals("CONFIRMED")
                ? "✅ Registration Confirmed — Bangla Karneval " + reg.getEventYear()
                : "❌ Registration Update — Bangla Karneval " + reg.getEventYear();
        sendHtmlEmail(
                reg.getEmail(),
                subject,
                emailTemplateUtil.buildRegistrationStatusEmail(reg, adminNote)
        );
    }

    // ── Performer status with admin note ─────────────────────────
    @Async
    public void sendPerformerStatusEmail(String toEmail, String name,
                                         String status, String adminNote, Integer eventYear) {
        String subject = "APPROVED".equals(status)
                ? "🎉 Performer Application Approved — Bangla Karneval " + eventYear
                : "📋 Performer Application Update — Bangla Karneval " + eventYear;
        sendHtmlEmail(
                toEmail,
                subject,
                emailTemplateUtil.buildPerformerStatusEmail(name, status, adminNote, eventYear)
        );
    }

}
