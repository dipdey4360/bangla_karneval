package com.bangla.karneval.service;

import com.bangla.karneval.model.PaymentStatus;
import com.bangla.karneval.model.Registration;
import com.bangla.karneval.repository.RegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentReminderService {

    @Autowired private RegistrationRepository registrationRepository;
    @Autowired private EmailService           emailService;

    public void sendRemindersAndMarkOverdue() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

        List<Registration> overdueList =
            registrationRepository.findOverduePayments(cutoffDate);

        for (Registration reg : overdueList) {
            reg.setPaymentStatus(PaymentStatus.OVERDUE);
            registrationRepository.save(reg);
            emailService.sendPaymentReminder(reg);
        }
    }
}
