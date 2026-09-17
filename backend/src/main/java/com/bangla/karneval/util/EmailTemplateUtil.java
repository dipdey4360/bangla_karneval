package com.bangla.karneval.util;

import com.bangla.karneval.model.Registration;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateUtil {

    private static final String BRAND_COLOR  = "#7B241C";
    private static final String RED          = "#C0392B";
    private static final String GOLD         = "#D4AC0D";
    private static final String GREEN        = "#27AE60";

    public String buildRegistrationConfirmationEmail(Registration reg) {
        Integer year = reg.getEventYear();
        String paymentDetails = buildPaymentDetails(reg);

        String body = "<h2 style=\"color:" + BRAND_COLOR + ";margin-bottom:16px\">Dear " + escHtml(reg.getPrimaryName()) + ",</h2>"
                + "<p style=\"font-size:16px;color:#444\">"
                + "Thank you for registering for <strong>Bangla Karneval" + (year == null ? "" : " " + year) + "</strong>! "
                + "Your registration has been received successfully.</p>"
                + "<div style=\"background:#f9f5ef;border-radius:8px;padding:20px;margin:24px 0;border-left:4px solid " + RED + "\">"
                + "<h3 style=\"color:" + BRAND_COLOR + ";margin-bottom:12px\">Registration Details</h3>"
                + "<table style=\"width:100%;font-size:15px\">"
                + "<tr><td style=\"padding:6px 0;color:#666\">Reference Code:</td>"
                + "<td style=\"padding:6px 0;font-weight:700;color:" + GREEN + ";font-size:18px;letter-spacing:2px\">"
                + escHtml(reg.getReferenceCode()) + "</td></tr>"
                + "<tr><td style=\"padding:6px 0;color:#666\">Participants:</td>"
                + "<td style=\"padding:6px 0;font-weight:600\">" + reg.getParticipantCount() + "</td></tr>"
                + "<tr><td style=\"padding:6px 0;color:#666\">Total Amount:</td>"
                + "<td style=\"padding:6px 0;font-weight:700;color:" + RED + ";font-size:18px\">€"
                + String.format("%.2f", reg.getCalculatedAmount()) + "</td></tr>"
                + "<tr><td style=\"padding:6px 0;color:#666\">Payment Method:</td>"
                + "<td style=\"padding:6px 0;font-weight:600\">"
                + (reg.getPaymentMethod() != null ? reg.getPaymentMethod().toString().replace("_", " ") : "")
                + "</td></tr>"
                + "</table></div>"
                + paymentDetails
                + "<p style=\"color:#666;font-size:14px;margin-top:24px\">"
                + "If you have any questions, please reply to this email or contact us via WhatsApp.</p>";

        return buildBase(body, year);
    }

    public String buildPaymentConfirmationEmail(Registration reg) {
        Integer year = reg.getEventYear();
        String body = "<h2 style=\"color:" + BRAND_COLOR + ";margin-bottom:16px\">Dear " + escHtml(reg.getPrimaryName()) + ",</h2>"
                + "<p style=\"font-size:16px;color:#444\">"
                + "Great news! Your payment for <strong>Bangla Karneval" + (year == null ? "" : " " + year) + "</strong> has been "
                + "<strong style=\"color:" + GREEN + "\">confirmed</strong>. Your spot is secured! 🎊</p>"
                + "<div style=\"background:#d4edda;border-radius:8px;padding:20px;margin:24px 0;border-left:4px solid " + GREEN + "\">"
                + "<h3 style=\"color:#155724;margin-bottom:12px\">✅ Payment Confirmed</h3>"
                + "<p style=\"margin:0;color:#155724\">"
                + "<strong>Reference:</strong> " + escHtml(reg.getReferenceCode()) + "<br>"
                + "<strong>Amount Paid:</strong> €" + String.format("%.2f", reg.getCalculatedAmount())
                + "</p></div>"
                + "<p style=\"color:#666\">We look forward to seeing you at the event. "
                + "Please keep your reference code handy.</p>";

        return buildBase(body, year);
    }

    public String buildPaymentReminderEmail(Registration reg) {
        Integer year = reg.getEventYear();
        String body = "<h2 style=\"color:" + BRAND_COLOR + ";margin-bottom:16px\">Dear " + escHtml(reg.getPrimaryName()) + ",</h2>"
                + "<p style=\"font-size:16px;color:#444\">"
                + "This is a friendly reminder that your payment for <strong>Bangla Karneval" + (year == null ? "" : " " + year) + "</strong> "
                + "is still <strong style=\"color:#E67E22\">pending</strong>.</p>"
                + "<div style=\"background:#fff3cd;border-radius:8px;padding:20px;margin:24px 0;border-left:4px solid #F39C12\">"
                + "<h3 style=\"color:#856404;margin-bottom:12px\">⚠️ Payment Pending</h3>"
                + "<p style=\"margin:0;color:#856404\">"
                + "<strong>Reference:</strong> " + escHtml(reg.getReferenceCode()) + "<br>"
                + "<strong>Amount Due:</strong> €" + String.format("%.2f", reg.getCalculatedAmount()) + "<br>"
                + "<strong>Payment Method:</strong> "
                + (reg.getPaymentMethod() != null ? reg.getPaymentMethod().toString().replace("_", " ") : "")
                + "</p></div>"
                + "<p style=\"color:#666\">"
                + "Please complete your payment as soon as possible to confirm your spot. "
                + "Unpaid registrations may be cancelled after the deadline.</p>";

        return buildBase(body, year);
    }

    private String buildPaymentDetails(Registration reg) {
        if (reg.getPaymentMethod() == null) return "";
        return switch (reg.getPaymentMethod()) {
            case PAYPAL ->
                    "<div style=\"background:#003087;border-radius:8px;padding:20px;color:#fff;margin-top:16px\">"
                            + "<h3 style=\"margin-bottom:12px;color:#fff\">🅿️ PayPal Payment Instructions</h3>"
                            + "<p style=\"margin:0\">Please send <strong>€"
                            + String.format("%.2f", reg.getCalculatedAmount())
                            + "</strong> via PayPal to:<br>"
                            + "<strong style=\"font-size:18px\">info@banglakarneval.com</strong><br>"
                            + "Include your reference code <strong>" + escHtml(reg.getReferenceCode())
                            + "</strong> in the payment note.</p></div>";

            case BANK_TRANSFER ->
                    "<div style=\"background:#1a5276;border-radius:8px;padding:20px;color:#fff;margin-top:16px\">"
                            + "<h3 style=\"margin-bottom:12px;color:#fff\">🏦 Bank Transfer Instructions</h3>"
                            + "<table style=\"width:100%;font-size:15px;color:#fff\">"
                            + "<tr><td style=\"padding:4px 0\">Account Holder:</td><td><strong>Bangla Karneval e.V.</strong></td></tr>"
                            + "<tr><td style=\"padding:4px 0\">IBAN:</td><td><strong>DE12 3456 7890 1234 5678 90</strong></td></tr>"
                            + "<tr><td style=\"padding:4px 0\">BIC:</td><td><strong>DEUTDEDB</strong></td></tr>"
                            + "<tr><td style=\"padding:4px 0\">Amount:</td><td><strong>€"
                            + String.format("%.2f", reg.getCalculatedAmount()) + "</strong></td></tr>"
                            + "<tr><td style=\"padding:4px 0\">Reference:</td><td><strong>"
                            + escHtml(reg.getReferenceCode()) + "</strong></td></tr>"
                            + "</table></div>";
        };
    }

    private String buildBase(String content, Integer year) {
        return "<!DOCTYPE html>"
                + "<html><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f4f0e8;font-family:'Segoe UI',Arial,sans-serif\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f0e8;padding:32px 0\">"
                + "<tr><td align=\"center\">"
                + "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#ffffff;border-radius:12px;overflow:hidden;"
                + "box-shadow:0 4px 16px rgba(0,0,0,0.1);max-width:600px;width:100%\">"
                // Header
                + "<tr><td style=\"background:" + BRAND_COLOR + ";padding:28px 32px;text-align:center\">"
                + "<h1 style=\"color:" + GOLD + ";margin:0;font-size:24px;font-family:Georgia,serif\">"
                + "বাংলা কার্নেভাল</h1>"
                + "<p style=\"color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:14px\">"
                + "Bangla Karneval" + (year == null ? "" : " " + year) + " — Wesseling, Germany</p>"
                + "</td></tr>"
                // Body
                + "<tr><td style=\"padding:32px\">" + content + "</td></tr>"
                // Footer
                + "<tr><td style=\"background:#f9f5ef;padding:20px 32px;text-align:center;"
                + "font-size:13px;color:#999;border-top:1px solid #f0e6d0\">"
                + "Bangla Karneval" + (year == null ? "" : " " + year) + " · Wesseling, Germany<br>"
                + "<a href=\"mailto:info@banglakarneval.com\" style=\"color:" + RED + "\">"
                + "info@banglakarneval.com</a>"
                + "</td></tr>"
                + "</table></td></tr></table>"
                + "</body></html>";
    }

    public String buildContactReplyEmail(String name, String originalMessage, String replyText) {
        Integer year = null;
        String body = "<h2 style='color:#7B241C'>Hello " + escHtml(name) + ",</h2>"
                + "<p>Thank you for reaching out to <strong>Bangla Karneval</strong>. "
                + "Here is our response to your message:</p>"
                + "<div style='background:#f9f5ef;border-left:4px solid #7B241C;"
                + "padding:12px 16px;border-radius:4px;margin:16px 0;color:#555;font-style:italic'>"
                + escHtml(originalMessage) + "</div>"
                + "<p><strong>Our Reply:</strong></p>"
                + "<div style='background:#fff8f0;border:1px solid #e0d0b0;"
                + "padding:14px 16px;border-radius:6px;margin:8px 0'>"
                + escHtml(replyText) + "</div>"
                + "<p style='margin-top:20px'>If you have further questions, feel free to reply to this email.</p>";
        return buildBase(body, year);
    }

    public String buildRegistrationStatusEmail(Registration reg, String adminNote) {
        Integer year = reg.getEventYear();
        boolean confirmed = reg.getPaymentStatus().name().equals("CONFIRMED");
        String statusColor = confirmed ? "#27AE60" : "#C0392B";
        String statusLabel = confirmed ? "✅ CONFIRMED" : "❌ " + reg.getPaymentStatus().name();
        String body = "<h2 style='color:#7B241C'>Hello " + escHtml(reg.getPrimaryName()) + ",</h2>"
                + "<p>Your registration for <strong>Bangla Karneval" + (year == null ? "" : " " + year) + "</strong> has been updated.</p>"
                + "<div style='background:#f9f5ef;padding:14px 16px;border-radius:6px;margin:16px 0'>"
                + "<strong>Reference:</strong> " + escHtml(reg.getReferenceCode()) + "<br>"
                + "<strong>Status:</strong> <span style='color:" + statusColor + ";font-weight:700'>"
                + statusLabel + "</span></div>"
                + (adminNote != null && !adminNote.isBlank()
                ? "<p><strong>Message from admin:</strong></p>"
                + "<div style='background:#fff8f0;border:1px solid #e0d0b0;"
                + "padding:12px 16px;border-radius:6px'>" + escHtml(adminNote) + "</div>"
                : "")
                + "<p style='margin-top:20px'>If you have questions, please contact us.</p>";
        return buildBase(body, year);
    }

    public String buildPerformerStatusEmail(String name, String status, String adminNote, Integer year) {
        boolean approved = "APPROVED".equals(status);
        String statusColor = approved ? "#27AE60" : "#C0392B";
        String statusLabel = approved ? "✅ APPROVED" : "❌ REJECTED";
        String body = "<h2 style='color:#7B241C'>Hello " + escHtml(name) + ",</h2>"
                + "<p>Your performer application for <strong>Bangla Karneval" + (year == null ? "" : " " + year) + "</strong> has been reviewed.</p>"
                + "<div style='background:#f9f5ef;padding:14px 16px;border-radius:6px;margin:16px 0'>"
                + "<strong>Decision:</strong> <span style='color:" + statusColor + ";font-weight:700'>"
                + statusLabel + "</span></div>"
                + (adminNote != null && !adminNote.isBlank()
                ? "<p><strong>Message from admin:</strong></p>"
                + "<div style='background:#fff8f0;border:1px solid #e0d0b0;"
                + "padding:12px 16px;border-radius:6px'>" + escHtml(adminNote) + "</div>"
                : "")
                + "<p style='margin-top:20px'>"
                + (approved ? "We look forward to your performance! Further details will follow."
                : "Thank you for your interest. We hope to see you at the event.")
                + "</p>";
        return buildBase(body, year);
    }


    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
