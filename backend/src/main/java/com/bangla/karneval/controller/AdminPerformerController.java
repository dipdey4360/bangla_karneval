package com.bangla.karneval.controller;

import com.bangla.karneval.dto.response.ApiResponse;
import com.bangla.karneval.model.PerformerRegistration;
import com.bangla.karneval.service.PerformerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/performers")
public class AdminPerformerController {

    @Autowired private PerformerService performerService;

    @GetMapping
    public ResponseEntity<List<PerformerRegistration>> getAll() {
        return ResponseEntity.ok(performerService.getAll());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status    = body.get("status");
        String adminNote = body.getOrDefault("adminNote", "");
        return ResponseEntity.ok(performerService.updateStatus(id, status, adminNote));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        performerService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Performer registration deleted"));
    }
}
