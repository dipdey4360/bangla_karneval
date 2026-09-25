package com.bangla.karneval.service;
import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.MemberRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class MembershipVerificationTest {
    @Mock MemberRepository members;
    @Mock ApplicationSettingsService settings;
    @InjectMocks MembershipVerificationService service;
    Member member;
    @BeforeEach void setup() {
        member = new Member(); member.setName("Alice Smith"); member.setPartnerName("Bob Smith");
        member.setMembershipType(Member.Type.COUPLE); member.setStatus(Member.Status.APPROVED); member.setPaymentVerified(true);
        member.setMembershipStartsOn(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Berlin")).minusMonths(1));
        member.setMembershipExpiresOn(member.getMembershipStartsOn().plusYears(1));
        when(members.findByMembershipId("BKM-00007")).thenReturn(Optional.of(member));
    }
    @Test void eitherPartnerCanVerifyWithSharedIdAndNormalizedName() {
        var config = new ApplicationSettings(); config.setMemberDiscountPercent(new BigDecimal("12.50"));
        when(settings.getSettings()).thenReturn(config);
        assertEquals(new BigDecimal("12.50"),service.verify(" bkm-00007 ","  ALICE   Smith ").discountPercent());
        assertTrue(service.verify("BKM-00007","Bob Smith").verified());
    }
    @Test void wrongNameUnknownIdAndInactiveMembershipDoNotRevealDetails() {
        var wrong = assertThrows(ResponseStatusException.class,()->service.verify("BKM-00007","Someone else"));
        var missing = assertThrows(ResponseStatusException.class,()->service.verify("BKM-99999","Alice Smith"));
        assertEquals(wrong.getReason(),missing.getReason());
        member.setStatus(Member.Status.REJECTED);
        assertThrows(ResponseStatusException.class,()->service.verify("BKM-00007","Alice Smith"));
        member.setStatus(Member.Status.APPROVED); member.setPaymentVerified(false);
        assertThrows(ResponseStatusException.class,()->service.verify("BKM-00007","Alice Smith"));
        verifyNoInteractions(settings);
    }
    @Test void singleMembershipDoesNotQualifyPartner() {
        member.setMembershipType(Member.Type.SINGLE);
        assertThrows(ResponseStatusException.class,()->service.verify("BKM-00007","Bob Smith"));
    }
    @Test void expiryDateAndMissingDatesCannotQualifyForDiscount() {
        member.setMembershipExpiresOn(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Berlin")));
        assertThrows(ResponseStatusException.class,()->service.verify("BKM-00007","Alice Smith"));
        assertEquals("Expired",member.getValidityStatus());
        member.setMembershipExpiresOn(null);
        assertThrows(ResponseStatusException.class,()->service.verify("BKM-00007","Alice Smith"));
        verifyNoInteractions(settings);
    }
}
