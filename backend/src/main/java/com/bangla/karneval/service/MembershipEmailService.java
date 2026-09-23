package com.bangla.karneval.service;

import com.bangla.karneval.model.Member;
import com.bangla.karneval.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MembershipEmailService {
    private static final Logger log = LoggerFactory.getLogger(MembershipEmailService.class);
    private final MemberRepository repository;
    private final JavaMailSender mail;
    private final String sender;
    public MembershipEmailService(MemberRepository repository, JavaMailSender mail, @Value("${spring.mail.username}") String sender) {
        this.repository = repository; this.mail = mail; this.sender = sender;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(Long id) {
        Member m = repository.findLockedById(id).orElse(null);
        if (m == null || m.getStatus() == Member.Status.PENDING || m.getEmailDelivery() != Member.Delivery.PENDING) return;
        boolean approved = m.getStatus() == Member.Status.APPROVED;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender); message.setTo(m.getEmail());
        if (approved && m.getMembershipType() == Member.Type.COUPLE && m.getPartnerEmail() != null
                && !m.getPartnerEmail().isBlank() && !m.getEmail().equalsIgnoreCase(m.getPartnerEmail()))
            message.setTo(m.getEmail(), m.getPartnerEmail());
        message.setSubject("Bangla Karneval — Membership " + (approved ? "approved" : "application update"));
        message.setText("Hello " + m.getName() + ",\n\n" + (approved
            ? "Your membership application has been approved. Welcome to Bangla Karneval!\n"
            : "Your membership application has been rejected. Please contact the organisers regarding any donation already made.\n")
            + (approved ? "\nMembership ID: " + m.getMembershipId()
                + (m.getMembershipExpiresOn() == null ? "" : "\nMembership starts: " + m.getMembershipStartsOn()
                    + "\nMembership expires: " + m.getMembershipExpiresOn()
                    + "\nWe will email you a reminder one calendar month before expiry.")
                + "\nKeep your membership ID confidential. You may need it to register for events throughout the year."
                + (m.getMembershipType() == Member.Type.COUPLE ? "\nThis ID is valid for both named members. Each partner should use their own full name when registering." : "") : "")
            + "\nMembership: " + m.getMembershipType() + "\nAnnual donation: EUR " + m.getAnnualFee()
            + (m.getAdminNote() == null || m.getAdminNote().isBlank() ? "" : "\n\nMessage from the board:\n" + m.getAdminNote())
            + "\n\nKind regards,\nBangla Karneval");
        try { mail.send(message); m.setEmailDelivery(Member.Delivery.SENT); }
        catch (RuntimeException e) { m.setEmailDelivery(Member.Delivery.FAILED); log.warn("Membership decision email failed for application {} ({})", id, e.getClass().getSimpleName()); }
        repository.save(m);
    }

    /** Row lock and separate recipient markers prevent duplicate sends during normal retries. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendExpiryReminder(Long id, java.time.LocalDate today) {
        Member m = repository.findLockedById(id).orElse(null);
        if (m == null || !m.isActiveOn(today) || today.isBefore(m.getMembershipExpiresOn().minusMonths(1))) return;
        if (m.getExpiryReminderSentAt() == null && sendReminder(m, m.getEmail(), m.getName()))
            m.setExpiryReminderSentAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Europe/Berlin")));
        if (m.getMembershipType() == Member.Type.COUPLE && m.getPartnerExpiryReminderSentAt() == null) {
            if (m.getPartnerEmail() != null && m.getPartnerEmail().equalsIgnoreCase(m.getEmail())) {
                m.setPartnerExpiryReminderSentAt(m.getExpiryReminderSentAt());
            } else if (sendReminder(m, m.getPartnerEmail(), m.getPartnerName())) {
                m.setPartnerExpiryReminderSentAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Europe/Berlin")));
            }
        }
        repository.save(m);
    }

    private boolean sendReminder(Member member, String recipient, String name) {
        if (recipient == null || recipient.isBlank()) return false;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender); message.setTo(recipient);
        message.setSubject("Bangla Karneval — Membership renewal reminder");
        message.setText("Hello " + name + ",\n\nYour Bangla Karneval membership expires on "
            + member.getMembershipExpiresOn() + ".\nMembership ID: " + member.getMembershipId()
            + "\n\nPlease contact the board to arrange your renewal and continue receiving member benefits."
            + "\nKeep your membership ID confidential."
            + "\n\nKind regards,\nBangla Karneval");
        try { mail.send(message); return true; }
        catch (RuntimeException e) {
            log.warn("Membership expiry reminder failed for application {} ({})", member.getId(), e.getClass().getSimpleName());
            return false;
        }
    }
}
