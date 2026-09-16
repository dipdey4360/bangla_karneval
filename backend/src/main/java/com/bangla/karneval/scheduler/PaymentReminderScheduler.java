package com.bangla.karneval.scheduler;

import com.bangla.karneval.service.PaymentReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentReminderScheduler {

    @Autowired private PaymentReminderService paymentReminderService;

    // Runs every day at 9:00 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void runDailyReminders() {
        System.out.println("Running daily payment reminder job...");
        paymentReminderService.sendRemindersAndMarkOverdue();
    }
}
