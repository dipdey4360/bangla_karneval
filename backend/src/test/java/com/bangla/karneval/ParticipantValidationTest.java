package com.bangla.karneval;

import com.bangla.karneval.dto.request.*;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ParticipantValidationTest {
    static ValidatorFactory factory;
    static Validator validator;
    @BeforeAll static void setup() { factory=Validation.buildDefaultValidatorFactory(); validator=factory.getValidator(); }
    @AfterAll static void close() { factory.close(); }
    GeneralRegistrationRequest general() {
        var r=new GeneralRegistrationRequest(); r.setEventYear(2026); r.setPrimaryName("Applicant"); r.setEmail("test@example.invalid");
        r.setPrimaryDateOfBirth(LocalDate.of(1990,1,1)); r.setPaymentMethod("PAYPAL"); r.setConsent(true); return r;
    }
    PerformerRegistrationRequest performer() {
        var r=new PerformerRegistrationRequest(); r.setEventYear(2026); r.setName("Performer"); r.setEmail("test@example.invalid"); r.setPerformanceType("DANCE"); r.setConsent(true); return r;
    }
    @Test void registrationRequiresExplicitConsent() {
        var r = general();
        r.setConsent(null); assertFalse(validator.validate(r).isEmpty());
        r.setConsent(false); assertFalse(validator.validate(r).isEmpty());
        r.setConsent(true); assertTrue(validator.validate(r).isEmpty());
    }
    @Test void additionalParticipantsValidateNamesDatesAndNulls() {
        var r=general(); assertTrue(validator.validate(r).isEmpty());
        r.setAdditionalParticipants(null); assertFalse(validator.validate(r).isEmpty());
        r.setAdditionalParticipants(Arrays.asList((GeneralRegistrationRequest.AdditionalParticipantRequest)null)); assertFalse(validator.validate(r).isEmpty());
        var p=new GeneralRegistrationRequest.AdditionalParticipantRequest();
        r.setAdditionalParticipants(List.of(p)); assertFalse(validator.validate(r).isEmpty());
        p.setName("Child"); p.setDateOfBirth(LocalDate.now().plusDays(1)); assertFalse(validator.validate(r).isEmpty());
        p.setDateOfBirth(LocalDate.now().minusYears(10)); assertTrue(validator.validate(r).isEmpty());
    }
    @Test void performerRequiresExplicitConsent() {
        var r = performer();
        r.setConsent(null); assertFalse(validator.validate(r).isEmpty());
        r.setConsent(false); assertFalse(validator.validate(r).isEmpty());
        r.setConsent(true); assertTrue(validator.validate(r).isEmpty());
    }
    @Test void performerGroupsValidateMembersAndCountIncludingMainPerformer() {
        var r=performer(); assertTrue(validator.validate(r).isEmpty());
        r.setGroupMembers(null); assertFalse(validator.validate(r).isEmpty());
        r.setGroupMembers(Arrays.asList((PerformerRegistrationRequest.GroupMemberRequest)null)); assertFalse(validator.validate(r).isEmpty());
        var p=new PerformerRegistrationRequest.GroupMemberRequest(); r.setGroupMembers(List.of(p));
        assertFalse(validator.validate(r).isEmpty()); p.setName("Partner");
        p.setDateOfBirth(LocalDate.now().plusDays(1)); assertFalse(validator.validate(r).isEmpty());
        p.setDateOfBirth(null); r.setGroupMemberCount(1); assertFalse(validator.validate(r).isEmpty());
        r.setGroupMemberCount(2); assertTrue(validator.validate(r).isEmpty());
        r.setGroupMemberCount(null); assertTrue(validator.validate(r).isEmpty());
        r.setGroupMemberCount(0); assertFalse(validator.validate(r).isEmpty());
    }
}
