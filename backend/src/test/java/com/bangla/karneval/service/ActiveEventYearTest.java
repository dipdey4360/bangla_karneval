package com.bangla.karneval.service;
import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.*;
import com.bangla.karneval.util.EmailTemplateUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ActiveEventYearTest {
    @Mock ApplicationSettingsRepository settings;
    @Mock EventConfigRepository configs;
    @Mock ProgrammeService programmes;
    @InjectMocks ApplicationSettingsService service;
    @Test void upgradeCopiesMembershipSettingsOnlyOnce() {
        var old=new EventConfig(); old.setMembershipCoupleFee(new BigDecimal("39.00")); old.setMembershipBenefits("Existing benefit");
        when(configs.findByEventYear(2026)).thenReturn(Optional.of(old));
        service.initialize();
        var row=ArgumentCaptor.forClass(ApplicationSettings.class); verify(settings).save(row.capture());
        assertEquals(2026,row.getValue().getActiveEventYear());
        assertEquals(old.getMembershipCoupleFee(),row.getValue().getMembershipCoupleFee());
        assertEquals("Existing benefit",row.getValue().getMembershipBenefits());
        when(settings.existsById(1)).thenReturn(true); service.initialize();
        verify(settings,times(1)).save(any());
    }
    @Test void activatingExistingYearPreservesMembershipAndRejectsStaleForms() {
        var row=new ApplicationSettings(); row.setActiveEventYear(2026); row.setMembershipSingleFee(new BigDecimal("25.00"));
        var future=new EventConfig(); future.setEventYear(2027);
        future.setLegacyEventYear(2027);
        when(programmes.legacyId(2027)).thenReturn(7L);
        when(programmes.get(7L)).thenReturn(future);
        when(settings.lockSettings()).thenReturn(Optional.of(row)); when(configs.findByEventYear(2027)).thenReturn(Optional.of(future));
        assertSame(future,service.activate(2027)); assertEquals(2027,row.getActiveEventYear());
        assertEquals(new BigDecimal("25.00"),row.getMembershipSingleFee());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.requireActiveYear(2026)).getStatusCode().value());
        assertSame(future,service.requireActiveYear(2027));
        assertThrows(ResponseStatusException.class,()->service.activate(2028)); assertEquals(2027,row.getActiveEventYear());
    }
    @Test void historicalEmailUsesRegistrationYear() {
        var templates=new EmailTemplateUtil(); var old=new Registration(); old.setEventYear(2024); old.setPrimaryName("Test");
        old.setCalculatedAmount(new BigDecimal("10.00")); old.setPaymentStatus(PaymentStatus.CONFIRMED);
        for(String html:new String[]{templates.buildRegistrationConfirmationEmail(old),templates.buildPaymentConfirmationEmail(old),templates.buildPaymentReminderEmail(old),templates.buildRegistrationStatusEmail(old,"")}) {
            assertTrue(html.contains("Bangla Karneval 2024")); assertFalse(html.contains("2026"));
        }
        assertTrue(templates.buildPerformerStatusEmail("Test","APPROVED","",2023).contains("Bangla Karneval 2023"));
        assertFalse(templates.buildContactReplyEmail("Test","Hi","Hello").contains("2026"));
    }
}
