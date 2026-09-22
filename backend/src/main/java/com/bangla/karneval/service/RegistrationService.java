package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.GeneralRegistrationRequest;
import com.bangla.karneval.dto.request.RegistrationUpdateRequest;
import com.bangla.karneval.dto.response.RegistrationResponse;
import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.*;
import com.bangla.karneval.util.ReferenceCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RegistrationService {
    public static final String CONSENT = "I consent to the storage and use of the submitted personal data to process and manage this event registration.";

    @Autowired private RegistrationRepository          registrationRepository;
    @Autowired private AdditionalParticipantRepository additionalParticipantRepository;
    @Autowired private EventConfigRepository           eventConfigRepository;
    @Autowired private PriceCalculationService         priceCalculationService;
    @Autowired private EmailService                    emailService;
    @Autowired private ApplicationSettingsService settings;

    @Transactional
    public RegistrationResponse registerParticipant(GeneralRegistrationRequest request) {
        if (!Boolean.TRUE.equals(request.getConsent()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Data storage consent is required");
        EventConfig config = request.getEventEditionId()==null ? settings.requireActiveYear(request.getEventYear()) : settings.requireActiveEdition(request.getEventEditionId(),request.getEventYear(),request.getEventVersion());
        if (Boolean.FALSE.equals(config.getRegistrationEnabled())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Registration is closed for this event.");

        BigDecimal pricePerPerson = config.getPricePerPerson();

        // Collect additional DOBs for price calculation
        List<LocalDate> additionalDobs = request.getAdditionalParticipants().stream()
                .map(GeneralRegistrationRequest.AdditionalParticipantRequest::getDateOfBirth)
                .toList();

        BigDecimal totalAmount = priceCalculationService.calculateTotalAmount(
                request.getPrimaryDateOfBirth(), additionalDobs, pricePerPerson
        );

        String referenceCode = ReferenceCodeGenerator.generate(config.getEventYear());

        Registration registration = new Registration();
        registration.setConsentAt(java.time.LocalDateTime.now());
        registration.setConsentText(CONSENT);
        registration.setPrimaryName(request.getPrimaryName());
        registration.setEmail(request.getEmail());
        registration.setPrimaryDateOfBirth(request.getPrimaryDateOfBirth()); // ← DOB
        registration.setAddress(request.getAddress());
        registration.setPhone(request.getPhone());
        registration.setParticipantCount(1 + request.getAdditionalParticipants().size());
        registration.setCalculatedAmount(totalAmount);
        registration.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()));
        registration.setReferenceCode(referenceCode);
        registration.setEventYear(config.getEventYear());
        registration.setEventEditionId(config.getEventEditionId());
        registration.setEventTitle(config.getTitle());
        registration.setEventDateSnapshot(config.getEventDate());
        registration.setEventLocationSnapshot(config.getEventLocation());
        registration.setPaymentInstructionsSnapshot(config.getPaymentInstructions());
        registration.setGender(request.getGender());

        registration = registrationRepository.save(registration);

        for (var p : request.getAdditionalParticipants()) {
            AdditionalParticipant ap = new AdditionalParticipant();
            ap.setRegistration(registration);
            ap.setName(p.getName());
            ap.setDateOfBirth(p.getDateOfBirth());
            ap.setGender(p.getGender());
            ap.setRelation(p.getRelation());
            additionalParticipantRepository.save(ap);
        }

        emailService.sendRegistrationConfirmation(registration);

        return new RegistrationResponse(
                registration.getId(), totalAmount, referenceCode,
                "Registration successful! Check your email for donation instructions."
        );
    }

    public List<Registration> searchRegistrations(String search, PaymentStatus status, int year) {
        return registrationRepository.findByEventYear(year).stream()
            .filter(r -> status == null || r.getPaymentStatus() == status)
            .filter(r -> search == null || search.isBlank() || r.getPrimaryName().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT)))
            .toList();
    }

    public List<Registration> searchByEdition(String search,PaymentStatus status,Long id) {
        return registrationRepository.findByEventEditionId(id).stream()
         .filter(r->status==null || r.getPaymentStatus()==status)
         .filter(r->search==null || search.isBlank() || r.getPrimaryName().toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))).toList();
    }
    public Registration getById(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration not found"));
    }

    @Transactional
    public Registration updateRegistration(Long id, RegistrationUpdateRequest updatedData) {
        Registration reg = getById(id);
        if (updatedData.paymentStatus() != null) reg.setPaymentStatus(updatedData.paymentStatus());
        if (updatedData.phone() != null) reg.setPhone(updatedData.phone());
        if (updatedData.email() != null) reg.setEmail(updatedData.email());
        if (updatedData.address() != null) reg.setAddress(updatedData.address());
        Registration saved = registrationRepository.save(reg);
        // Preserve the dashboard's existing update notification and optional admin note.
        emailService.sendRegistrationStatusEmail(saved, updatedData.adminNote() == null ? "" : updatedData.adminNote());
        return saved;
    }

    @Transactional
    public void updatePaymentStatus(Long id, PaymentStatus newStatus) {
        Registration reg = getById(id);
        reg.setPaymentStatus(newStatus);
        registrationRepository.save(reg);
        if (newStatus == PaymentStatus.CONFIRMED) {
            emailService.sendPaymentConfirmation(reg);
        }
    }

    @Transactional
    public void deleteRegistration(Long id) {
        registrationRepository.deleteById(id);
    }

    public List<Registration> getAllForYear(int year) {
        return registrationRepository.findByEventYear(year);
    }
}
