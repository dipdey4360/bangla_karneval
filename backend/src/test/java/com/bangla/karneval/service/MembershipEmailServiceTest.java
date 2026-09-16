package com.bangla.karneval.service;
import com.bangla.karneval.model.Member;
import com.bangla.karneval.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MembershipEmailServiceTest {
    @Test void sendsBothDecisionsAndRecordsDeliveryWithoutNetwork() {
        for (Member.Status status : new Member.Status[]{Member.Status.APPROVED,Member.Status.REJECTED}) {
            var repository=mock(MemberRepository.class); var mail=mock(JavaMailSender.class);
            Member m=new Member(); m.setId(1L); m.setName("Alice"); m.setEmail("alice@example.invalid"); m.setStatus(status); m.setEmailDelivery(Member.Delivery.PENDING);
            when(repository.findLockedById(1L)).thenReturn(Optional.of(m));
            var service=new MembershipEmailService(repository,mail,"sender@example.invalid");
            service.deliver(1L); service.deliver(1L);
            verify(mail,times(1)).send(argThat((SimpleMailMessage msg)->msg.getText().contains(status==Member.Status.APPROVED?"approved":"rejected")));
            assertEquals(Member.Delivery.SENT,m.getEmailDelivery());
        }
    }
    @Test void smtpFailureRemainsVisibleForRetry() {
        var repository=mock(MemberRepository.class); var mail=mock(JavaMailSender.class);
        Member m=new Member(); m.setStatus(Member.Status.REJECTED); m.setEmail("a@example.invalid"); m.setEmailDelivery(Member.Delivery.PENDING);
        when(repository.findLockedById(1L)).thenReturn(Optional.of(m));
        doThrow(new MailSendException("Test failure")).when(mail).send(any(SimpleMailMessage.class));
        new MembershipEmailService(repository,mail,"sender@example.invalid").deliver(1L);
        assertEquals(Member.Delivery.FAILED,m.getEmailDelivery());
    }
}
