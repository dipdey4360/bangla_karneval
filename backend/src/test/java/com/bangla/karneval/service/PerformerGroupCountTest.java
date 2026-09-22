package com.bangla.karneval.service;
import com.bangla.karneval.dto.request.PerformerRegistrationRequest;
import com.bangla.karneval.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class PerformerGroupCountTest {
    @Mock PerformerRegistrationRepository performerRepository;
    @Mock PerformerGroupMemberRepository groupMemberRepository;
    @Mock EmailService emailService;
    @Mock ApplicationSettingsService settings;
    @InjectMocks PerformerService service;
    @Test void rejectsMissingOrDeclinedConsent() {
        var r = new PerformerRegistrationRequest();
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.register(r));
        r.setConsent(false);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.register(r));
        verifyNoInteractions(performerRepository, groupMemberRepository, settings);
    }
    @Test void derivesCountInsteadOfTrustingClient() {
        var r=new PerformerRegistrationRequest(); r.setEventYear(2026); r.setConsent(true);
        var config=new com.bangla.karneval.model.EventConfig(); config.setEventYear(2026);
        when(settings.requireActiveYear(2026)).thenReturn(config); r.setName("Lead"); r.setGroupMemberCount(99);
        var member=new PerformerRegistrationRequest.GroupMemberRequest(); member.setName("Partner"); r.setGroupMembers(List.of(member));
        when(performerRepository.save(any())).thenAnswer(i->i.getArgument(0));
        var saved = service.register(r);
        assertEquals(2,saved.getGroupMemberCount());
        assertNotNull(saved.getConsentAt());
        assertEquals(PerformerService.CONSENT, saved.getConsentText()); verify(groupMemberRepository).save(any());
    }
}
