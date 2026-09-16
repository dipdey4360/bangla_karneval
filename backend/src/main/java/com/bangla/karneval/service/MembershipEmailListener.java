package com.bangla.karneval.service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
@Component
public class MembershipEmailListener {
    private final MembershipEmailService email;
    public MembershipEmailListener(MembershipEmailService email) { this.email = email; }
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterDecision(MembershipService.DecisionEmail event) { email.deliver(event.memberId()); }
}
