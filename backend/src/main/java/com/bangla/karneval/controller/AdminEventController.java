package com.bangla.karneval.controller;

import com.bangla.karneval.dto.request.EventRequest;
import com.bangla.karneval.dto.response.ApiResponse;
import com.bangla.karneval.model.Event;
import com.bangla.karneval.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    @Autowired private EventService eventService;
    @Autowired private com.bangla.karneval.service.ApplicationSettingsService settings;
    @Autowired private com.bangla.karneval.service.ProgrammeService programmes;

    @GetMapping
    public ResponseEntity<List<Event>> getAll(@RequestParam(required = false) Integer year, @RequestParam(required = false) Long eventEditionId) {
        return ResponseEntity.ok(eventService.getByEdition(programmes.resolve(eventEditionId,year)));
    }

    @PostMapping
    public ResponseEntity<Event> create(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Event deleted"));
    }
}
