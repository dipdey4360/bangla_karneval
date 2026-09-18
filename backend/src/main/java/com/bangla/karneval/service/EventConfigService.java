package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.ConfigUpdateRequest;
import com.bangla.karneval.model.EventConfig;
import com.bangla.karneval.repository.EventConfigRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventConfigService {

    @Autowired private EventConfigRepository eventConfigRepository;
    @Autowired private ApplicationSettingsService settings;
    @Autowired private ProgrammeService programmes;

    @PersistenceContext
    private EntityManager entityManager;

    public EventConfig getCurrentYearConfig() {
        return programmes.current();
    }

    @Transactional
    public EventConfig update(ConfigUpdateRequest request) {
        EventConfig config = eventConfigRepository.findByEventYear(request.getEventYear())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Event year not found"));
        if (request.getPricePerPerson()  != null) config.setPricePerPerson(request.getPricePerPerson());
        if (request.getEventDate()       != null) config.setEventDate(request.getEventDate());
        if (request.getEventLocation()   != null) config.setEventLocation(request.getEventLocation());
        if (request.getAboutText()       != null) config.setAboutText(request.getAboutText());
        if (request.getContactPhone()    != null) config.setContactPhone(request.getContactPhone());
        if (request.getContactEmail()    != null) config.setContactEmail(request.getContactEmail());
        if (request.getContactWhatsapp() != null) config.setContactWhatsapp(request.getContactWhatsapp());
        if (request.getContactFacebook() != null) config.setContactFacebook(request.getContactFacebook());
        if (request.getContactInstagram()!= null) config.setContactInstagram(request.getContactInstagram());
        return eventConfigRepository.save(config);
    }

    /* ── Read methods — clear L1 cache first to avoid stale reads ── */

    public boolean isPerformerEnabled() { return programmes.current().getPerformerEnabled(); }

    public boolean isRegistrationEnabled() { return programmes.current().getRegistrationEnabled(); }

    /* ── Toggle methods — compute new value explicitly, never re-read ── */

    @Transactional
    public boolean togglePerformerEnabled() { return programmes.toggle(true); }

    @Transactional
    public boolean toggleRegistrationEnabled() { return programmes.toggle(false); }
}
