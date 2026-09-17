package com.bangla.karneval.controller;

import com.bangla.karneval.dto.request.ConfigUpdateRequest;
import com.bangla.karneval.model.EventConfig;
import com.bangla.karneval.repository.EventConfigRepository;
import com.bangla.karneval.service.EventConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    @Autowired private EventConfigService    eventConfigService;
    @Autowired private EventConfigRepository eventConfigRepository; // ← added
    @Autowired private com.bangla.karneval.service.ApplicationSettingsService settings;

    @GetMapping("/year/{year}")
    public ResponseEntity<EventConfig> getYear(@PathVariable Integer year) {
        return ResponseEntity.of(eventConfigRepository.findByEventYear(year));
    }
    public record ActiveYear(@jakarta.validation.constraints.NotNull Integer eventYear) {}
    @PutMapping("/active-year")
    @PreAuthorize("hasRole('ADMIN')")
    public EventConfig activate(@jakarta.validation.Valid @RequestBody ActiveYear request) {
        return settings.activate(request.eventYear());
    }

    @PutMapping
    public ResponseEntity<EventConfig> update(@jakarta.validation.Valid @RequestBody ConfigUpdateRequest request) {
        return ResponseEntity.ok(eventConfigService.update(request));
    }

    @GetMapping("/performer-status")
    public ResponseEntity<Map<String, Boolean>> getPerformerStatus() {
        return ResponseEntity.ok(Map.of("enabled", eventConfigService.isPerformerEnabled()));
    }

    @GetMapping("/registration-status")
    public ResponseEntity<Map<String, Boolean>> getRegistrationStatus() {
        return ResponseEntity.ok(Map.of("enabled", eventConfigService.isRegistrationEnabled()));
    }

    @PatchMapping("/performer-toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> togglePerformer() {
        boolean enabled = eventConfigService.togglePerformerEnabled();
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @PatchMapping("/registration-toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> toggleRegistration() {
        boolean enabled = eventConfigService.toggleRegistrationEnabled();
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    /* ── Add a new event year (for gallery uploads) ─────────── */
    @PostMapping("/year")
    public ResponseEntity<EventConfig> addYear(@RequestBody Map<String, Integer> body) {
        Integer year = body.get("eventYear");
        if (year == null) {
            return ResponseEntity.badRequest().build();
        }
        // Return existing if already present — idempotent
        var existing = eventConfigRepository.findByEventYear(year);
        if (existing.isPresent()) {
            return ResponseEntity.ok(existing.get());
        }
        EventConfig config = new EventConfig();
        config.setEventYear(year);
        return ResponseEntity.ok(eventConfigRepository.save(config));
    }
}
