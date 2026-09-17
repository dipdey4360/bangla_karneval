package com.bangla.karneval.service;

import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class ApplicationSettingsService {
    private final ApplicationSettingsRepository settings;
    private final EventConfigRepository configs;
    public ApplicationSettingsService(ApplicationSettingsRepository settings, EventConfigRepository configs) {
        this.settings=settings; this.configs=configs;
    }
    @Transactional
    public void initialize() {
        if (settings.existsById(1)) return;
        var row=new ApplicationSettings();
        // One-time upgrade default, never used to select an event after initialization.
        row.setActiveEventYear(2026);
        configs.findByEventYear(2026).ifPresent(old -> {
            row.setMembershipBenefits(old.getMembershipBenefits());
            row.setMembershipSingleFee(old.getMembershipSingleFee());
            row.setMembershipCoupleFee(old.getMembershipCoupleFee());
            row.setMembershipPaymentInstructions(old.getMembershipPaymentInstructions());
        });
        settings.save(row);
    }
    public ApplicationSettings getSettings() {
        return settings.findById(1).orElseThrow(() -> new ResponseStatusException(SERVICE_UNAVAILABLE,"Application settings are not ready"));
    }
    public int activeYear() { return getSettings().getActiveEventYear(); }
    @Transactional
    public EventConfig activate(int year) {
        var row=settings.lockSettings().orElseThrow();
        var config=configs.findByEventYear(year).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST,"Create and configure this event year before activating it"));
        row.setActiveEventYear(year);
        return config;
    }
    // Lock serializes activation with in-flight registrations. The outer registration transaction retains it.
    @Transactional
    public EventConfig requireActiveYear(Integer submittedYear) {
        int active=settings.lockSettings().orElseThrow().getActiveEventYear();
        if (submittedYear == null || submittedYear != active)
            throw new ResponseStatusException(CONFLICT,"The active event year has changed. Refresh the page and review the event details before submitting again.");
        return configs.findByEventYear(active).orElseThrow(() -> new ResponseStatusException(SERVICE_UNAVAILABLE,"Active event configuration is missing"));
    }
}
