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

    @PersistenceContext
    private EntityManager entityManager;

    public EventConfig getCurrentYearConfig() {
        return eventConfigRepository.findByEventYear(2026)
                .orElseThrow(() -> new RuntimeException("Event config not found"));
    }

    @Transactional
    public EventConfig update(ConfigUpdateRequest request) {
        EventConfig config = getCurrentYearConfig();
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

    public boolean isPerformerEnabled() {
        entityManager.clear();
        return eventConfigRepository.findByEventYear(2026)
                .map(c -> Boolean.TRUE.equals(c.getPerformerEnabled()))
                .orElse(true);
    }

    public boolean isRegistrationEnabled() {
        entityManager.clear();
        return eventConfigRepository.findByEventYear(2026)
                .map(c -> Boolean.TRUE.equals(c.getRegistrationEnabled()))
                .orElse(true);
    }

    /* ── Toggle methods — compute new value explicitly, never re-read ── */

    @Transactional
    public boolean togglePerformerEnabled() {
        EventConfig config = eventConfigRepository.findByEventYear(2026)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        boolean newValue = !Boolean.TRUE.equals(config.getPerformerEnabled());
        config.setPerformerEnabled(newValue);
        eventConfigRepository.saveAndFlush(config);
        return newValue;
    }

    @Transactional
    public boolean toggleRegistrationEnabled() {
        EventConfig config = eventConfigRepository.findByEventYear(2026)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        boolean newValue = !Boolean.TRUE.equals(config.getRegistrationEnabled());
        config.setRegistrationEnabled(newValue);
        eventConfigRepository.saveAndFlush(config);
        return newValue;
    }
}
