package com.bangla.karneval.service;

import com.bangla.karneval.model.Member;
import com.bangla.karneval.repository.MemberRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipExpiryTest {
    @Mock MemberRepository repository;
    @Mock JavaMailSender mail;
    MembershipEmailService service;
    Member member;
    @BeforeEach void setup() {
        service = new MembershipEmailService(repository, mail, "board@example.invalid");
        member = new Member(); member.setId(1L); member.setMembershipId("BKM-00001");
        member.setName("Alice"); member.setEmail("alice@example.invalid");
        member.setPartnerName("Bob"); member.setPartnerEmail("bob@example.invalid");
        member.setMembershipType(Member.Type.COUPLE); member.setStatus(Member.Status.APPROVED); member.setPaymentVerified(true);
        member.setMembershipStartsOn(LocalDate.of(2026,3,31)); member.setMembershipExpiresOn(LocalDate.of(2027,3,31));
        when(repository.findLockedById(1L)).thenReturn(Optional.of(member));
    }
    @Test void calendarMonthEndSendsToBothOnceAndIncludesIdAndExpiry() {
        service.sendExpiryReminder(1L,LocalDate.of(2027,2,27));
        verifyNoInteractions(mail);
        assertEquals(LocalDate.of(2027,2,28),member.getExpiryReminderDueOn());
        service.sendExpiryReminder(1L,LocalDate.of(2027,2,28));
        service.sendExpiryReminder(1L,LocalDate.of(2027,3,1));
        var messages=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail,times(2)).send(messages.capture());
        assertEquals("alice@example.invalid",messages.getAllValues().get(0).getTo()[0]);
        assertEquals("bob@example.invalid",messages.getAllValues().get(1).getTo()[0]);
        assertTrue(messages.getValue().getText().contains("2027-03-31"));
        assertTrue(messages.getValue().getText().contains("BKM-00001"));
        assertNotNull(member.getExpiryReminderSentAt()); assertNotNull(member.getPartnerExpiryReminderSentAt());
    }
    @Test void failedPartnerRetriesWithoutResendingPrimary() {
        var partnerAttempts = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(call -> {
            SimpleMailMessage message = call.getArgument(0);
            if (message.getTo()[0].equals("bob@example.invalid") && partnerAttempts.getAndIncrement() == 0)
                throw new MailSendException("test failure");
            return null;
        }).when(mail).send(any(SimpleMailMessage.class));
        service.sendExpiryReminder(1L,LocalDate.of(2027,3,1));
        assertNotNull(member.getExpiryReminderSentAt()); assertNull(member.getPartnerExpiryReminderSentAt());
        service.sendExpiryReminder(1L,LocalDate.of(2027,3,2));
        assertNotNull(member.getPartnerExpiryReminderSentAt());
        verify(mail,times(1)).send(argThat((SimpleMailMessage message)->message.getTo()[0].equals("alice@example.invalid")));
        verify(mail,times(2)).send(argThat((SimpleMailMessage message)->message.getTo()[0].equals("bob@example.invalid")));
    }
    @Test void sharedAddressReceivesOneMessage() {
        member.setPartnerEmail("ALICE@example.invalid");
        service.sendExpiryReminder(1L,LocalDate.of(2027,3,1));
        service.sendExpiryReminder(1L,LocalDate.of(2027,3,2));
        verify(mail,times(1)).send(any(SimpleMailMessage.class));
        assertEquals(member.getExpiryReminderSentAt(),member.getPartnerExpiryReminderSentAt());
    }
    @Test void rejectedUnverifiedExpiredAndUnknownDatesDoNotSend() {
        member.setStatus(Member.Status.REJECTED); service.sendExpiryReminder(1L,LocalDate.of(2027,3,1));
        member.setStatus(Member.Status.APPROVED); member.setPaymentVerified(false); service.sendExpiryReminder(1L,LocalDate.of(2027,3,1));
        member.setPaymentVerified(true); service.sendExpiryReminder(1L,LocalDate.of(2027,3,31));
        member.setMembershipExpiresOn(null); service.sendExpiryReminder(1L,LocalDate.of(2027,3,1));
        verifyNoInteractions(mail);
    }
    @Test void singleMemberCatchesUpAfterMissedRun() {
        member.setMembershipType(Member.Type.SINGLE);
        service.sendExpiryReminder(1L,LocalDate.of(2027,3,20));
        verify(mail,times(1)).send(any(SimpleMailMessage.class));
        assertNotNull(member.getExpiryReminderSentAt()); assertNull(member.getPartnerExpiryReminderSentAt());
    }
}
