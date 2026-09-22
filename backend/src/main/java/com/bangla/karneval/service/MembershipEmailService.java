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
        message.setSubject("Bangla Karneval — Membership " + (approved ? "approved" : "application update"));
        message.setText("Hello " + m.getName() + ",\n\n" + (approved
            ? "Your membership application has been approved. Welcome to Bangla Karneval!\n"
            : "Your membership application has been rejected. Please contact the organisers regarding any donation already made.\n")
            + "\nMembership: " + m.getMembershipType() + "\nAnnual donation: EUR " + m.getAnnualFee()
            + (m.getAdminNote() == null || m.getAdminNote().isBlank() ? "" : "\n\nMessage from the board:\n" + m.getAdminNote())
            + "\n\nKind regards,\nBangla Karneval");
        try { mail.send(message); m.setEmailDelivery(Member.Delivery.SENT); }
        catch (RuntimeException e) { m.setEmailDelivery(Member.Delivery.FAILED); log.warn("Membership decision email failed for application {} ({})", id, e.getClass().getSimpleName()); }
        repository.save(m);
    }
}
