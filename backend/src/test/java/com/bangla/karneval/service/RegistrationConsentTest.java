package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.GeneralRegistrationRequest;
import com.bangla.karneval.model.EventConfig;
import com.bangla.karneval.model.Registration;
import com.bangla.karneval.repository.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationConsentTest {
    @Mock RegistrationRepository registrationRepository;
    @Mock ApplicationSettingsService settings;
    @Mock PriceCalculationService priceCalculationService;
    @Mock EmailService emailService;
    @InjectMocks RegistrationService service;

    @Test void missingOrDeclinedConsentCannotCreateARegistration() {
        var request = new GeneralRegistrationRequest();
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> service.registerParticipant(request)).getStatusCode().value());
        request.setConsent(false);
        assertThrows(ResponseStatusException.class, () -> service.registerParticipant(request));
        verifyNoInteractions(registrationRepository, settings, emailService);
    }

    @Test void acceptedConsentIsRecordedWithTheRegistration() {
        var request = new GeneralRegistrationRequest();
        request.setConsent(true); request.setEventYear(2026); request.setPaymentMethod("PAYPAL");
        var config = new EventConfig(); config.setEventYear(2026); config.setPricePerPerson(BigDecimal.TEN);
        when(settings.requireActiveYear(2026)).thenReturn(config);
        when(priceCalculationService.calculateTotalAmount(any(), any(), any())).thenReturn(BigDecimal.TEN);
        when(registrationRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        service.registerParticipant(request);
        var saved = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(saved.capture());
        assertNotNull(saved.getValue().getConsentAt());
        assertEquals(RegistrationService.CONSENT, saved.getValue().getConsentText());
    }
}
