package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.*;
import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.MemberRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {
    @Mock MemberRepository repository;
    @Mock ApplicationSettingsService config;
    @Mock ApplicationEventPublisher events;
    @InjectMocks MembershipService service;
    @Test void deletesWholeCoupleRecordWithoutSendingEmail() {
        Member member = new Member(); member.setId(7L); member.setMembershipType(Member.Type.COUPLE);
        member.setName("Alice"); member.setPartnerName("Bob");
        when(repository.findLockedById(7L)).thenReturn(Optional.of(member));
        service.delete(7L);
        verify(repository).delete(member);
        verifyNoInteractions(events);
    }
    @Test void missingMemberCannotBeDeleted() {
        when(repository.findLockedById(7L)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.delete(7L));
        assertEquals(404, error.getStatusCode().value());
        verify(repository, never()).delete(any(Member.class));
        verifyNoInteractions(events);
    }
    MembershipRequest request(Member.Type type, String partner, boolean consent) {
        return new MembershipRequest("Alice",partner,LocalDate.of(1991,2,3),"Partner street","987654","bob@example.invalid",LocalDate.of(1990,1,1),"Example street","123456","alice@example.invalid",type,PaymentMethod.BANK_TRANSFER,true,consent,true);
    }
    @Test void singleAndCoupleFeesAreCalculatedByServer() {
        when(config.getSettings()).thenReturn(new ApplicationSettings());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        Member single=service.apply(request(Member.Type.SINGLE,null,true));
        Member couple=service.apply(request(Member.Type.COUPLE,"Bob",true));
        assertEquals(new BigDecimal("25.00"),single.getAnnualFee());
        assertEquals(new BigDecimal("30.00"),couple.getAnnualFee());
        assertEquals(Member.Status.PENDING,couple.getStatus());
        assertFalse(couple.isPaymentVerified());
        assertEquals("Bob",couple.getPartnerName());
        assertEquals(LocalDate.of(1991,2,3),couple.getPartnerDateOfBirth());
        assertEquals("Partner street",couple.getPartnerAddress());
        assertEquals("987654",couple.getPartnerPhone());
        assertEquals("bob@example.invalid",couple.getPartnerEmail());
        assertNull(single.getPartnerEmail());
        assertNull(single.getPartnerDateOfBirth());
        assertNotNull(couple.getConsentAt());
        verifyNoInteractions(events);
    }
    @Test void consentAndCouplePartnerAreRequired() {
        var incomplete = new MembershipRequest("Alice","Bob",null,null,null,null,LocalDate.of(1990,1,1),"Street","123","alice@example.invalid",Member.Type.COUPLE,PaymentMethod.PAYPAL,true,true,true);
        assertThrows(ResponseStatusException.class,()->service.apply(incomplete));
        assertThrows(ResponseStatusException.class,()->service.apply(request(Member.Type.SINGLE,null,false)));
        assertThrows(ResponseStatusException.class,()->service.apply(request(Member.Type.COUPLE," ",true)));
        verifyNoInteractions(repository);
    }
    @Test void applicantVisibilityChoiceIsRequiredAndPreservedOnApproval() {
        when(config.getSettings()).thenReturn(new ApplicationSettings());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        for (boolean listed : new boolean[]{false,true}) {
            var request = new MembershipRequest("Alice","Bob",LocalDate.of(1991,2,3),"Street","123","bob@example.invalid",LocalDate.of(1990,1,1),"Street","123","alice@example.invalid",Member.Type.COUPLE,PaymentMethod.PAYPAL,true,true,listed);
            Member m = service.apply(request); m.setId(1L);
            assertEquals(listed,m.isListed());
            when(repository.findLockedById(1L)).thenReturn(Optional.of(m));
            service.decide(1L,new MembershipDecisionRequest(Member.Status.APPROVED,true,""));
            assertEquals(listed,m.isListed());
        }
        var missing = new MembershipRequest("Alice",null,null,null,null,null,LocalDate.of(1990,1,1),"Street","123","alice@example.invalid",Member.Type.SINGLE,PaymentMethod.PAYPAL,true,true,null);
        assertThrows(ResponseStatusException.class,()->service.apply(missing));
    }
    @Test void approvalRequiresVerifiedPaymentAndPublishesOneEvent() {
        Member m=new Member(); m.setId(1L);
        when(repository.findLockedById(1L)).thenReturn(Optional.of(m));
        when(repository.save(m)).thenReturn(m);
        assertThrows(ResponseStatusException.class,()->service.decide(1L,new MembershipDecisionRequest(Member.Status.APPROVED,false,"")));
        verifyNoInteractions(events);
        service.decide(1L,new MembershipDecisionRequest(Member.Status.APPROVED,true,"Welcome"));
        assertEquals(Member.Delivery.PENDING,m.getEmailDelivery());
        assertEquals("Welcome",m.getAdminNote());
        assertNotNull(m.getReviewedAt());
        service.decide(1L,new MembershipDecisionRequest(Member.Status.APPROVED,true,"Welcome"));
        verify(events,times(1)).publishEvent(new MembershipService.DecisionEmail(1L));
    }
    @Test void rejectionDoesNotRequirePaymentAndCannotResetToPending() {
        Member m=new Member(); m.setId(2L);
        when(repository.findLockedById(2L)).thenReturn(Optional.of(m));
        when(repository.save(m)).thenReturn(m);
        service.decide(2L,new MembershipDecisionRequest(Member.Status.REJECTED,false,"Please contact us"));
        assertEquals(Member.Status.REJECTED,m.getStatus());
        verify(events).publishEvent(new MembershipService.DecisionEmail(2L));
        assertThrows(ResponseStatusException.class,()->service.decide(2L,new MembershipDecisionRequest(Member.Status.PENDING,false,"")));
    }
    @Test void publicProjectionContainsNamesOnlyIncludingPartner() {
        Member m=new Member(); m.setName("Alice"); m.setPartnerName("Bob"); m.setMembershipType(Member.Type.COUPLE); m.setEmail("private@example.invalid");
        when(repository.findByStatusAndListedTrueOrderByNameAsc(Member.Status.APPROVED)).thenReturn(List.of(m));
        var names=service.publicMembers();
        assertEquals(List.of(new MembershipService.PublicMember("Alice"),new MembershipService.PublicMember("Bob")),names);
        assertEquals(1,MembershipService.PublicMember.class.getRecordComponents().length);
    }
    @Test void settingsChangesDoNotRepriceExistingApplications() {
        ApplicationSettings c=new ApplicationSettings(); when(config.getSettings()).thenReturn(c);
        service.updateSettings(new MembershipSettingsRequest("Voting",new BigDecimal("26.00"),new BigDecimal("31.00"),"Instructions"));
        assertEquals(new BigDecimal("26.00"),service.settings().singleFee());
        verifyNoInteractions(repository);
    }
}
