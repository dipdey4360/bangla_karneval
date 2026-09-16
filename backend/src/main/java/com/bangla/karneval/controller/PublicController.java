package com.bangla.karneval.controller;

import com.bangla.karneval.model.Event;
import com.bangla.karneval.model.EventConfig;
import com.bangla.karneval.model.GalleryItem;
import com.bangla.karneval.service.EventService;
import com.bangla.karneval.service.EventConfigService;
import com.bangla.karneval.service.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class PublicController {

    @Autowired private EventService       eventService;
    @Autowired private GalleryService     galleryService;
    @Autowired private EventConfigService eventConfigService;

    @GetMapping("/api/events/{year}")
    public ResponseEntity<List<Event>> getEvents(@PathVariable Integer year) {
        return ResponseEntity.ok(eventService.getByYear(year));
    }

    @GetMapping("/api/gallery/{year}")
    public ResponseEntity<List<GalleryItem>> getGallery(
            @PathVariable Integer year,
            @RequestParam(required = false) Boolean highlight) {
        if (Boolean.TRUE.equals(highlight)) {
            return ResponseEntity.ok(galleryService.getHighlights(year));
        }
        return ResponseEntity.ok(galleryService.getByYear(year));
    }

    @GetMapping("/api/config/current")
    public ResponseEntity<EventConfig> getCurrentConfig() {
        return ResponseEntity.ok(eventConfigService.getCurrentYearConfig());
    }

    @GetMapping("/api/gallery/years")
    public ResponseEntity<List<Integer>> getGalleryYears() {
        return ResponseEntity.ok(galleryService.getGalleryYears());
    }

    @GetMapping("/api/config/button-status")
    public ResponseEntity<Map<String, Boolean>> getButtonStatus() {
        return ResponseEntity.ok(Map.of(
                "registrationEnabled", eventConfigService.isRegistrationEnabled(),
                "performerEnabled",    eventConfigService.isPerformerEnabled()
        ));
    }
}
