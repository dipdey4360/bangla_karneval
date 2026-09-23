package com.bangla.karneval.scheduler;

import com.bangla.karneval.model.Member;
import com.bangla.karneval.repository.MemberRepository;
import com.bangla.karneval.service.MembershipEmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class MembershipExpiryScheduler {
    private static final Logger log = LoggerFactory.getLogger(MembershipExpiryScheduler.class);
    private final MemberRepository members;
    private final MembershipEmailService emails;
    public MembershipExpiryScheduler(MemberRepository members, MembershipEmailService emails) {
        this.members = members; this.emails = emails;
    }
    @Scheduled(cron = "0 0 9 * * *", zone = "Europe/Berlin")
    public void runDailyReminders() {
        sendDueReminders(LocalDate.now(ZoneId.of("Europe/Berlin")));
    }
    public void sendDueReminders(LocalDate today) {
        // Wide candidate window covers month-end clamping; the service checks expiry.minusMonths(1).
        for (Long id : members.findExpiryReminderCandidates(Member.Status.APPROVED, Member.Type.COUPLE, today, today.plusDays(32))) {
            try { emails.sendExpiryReminder(id, today); }
            catch (RuntimeException e) { log.warn("Membership expiry reminder processing failed for application {} ({})", id, e.getClass().getSimpleName()); }
        }
    }
}
