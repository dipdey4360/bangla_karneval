package com.bangla.karneval.controller;

import com.bangla.karneval.model.Sponsor;
import com.bangla.karneval.service.SponsorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SponsorController {

    @Autowired private SponsorService sponsorService;

    // ── Public ──────────────────────────────────────────────────────────────
    @GetMapping("/sponsors")
    public ResponseEntity<List<Sponsor>> getVisible() {
        return ResponseEntity.ok(sponsorService.getVisibleSponsors());
    }

    // ── Admin ────────────────────────────────────────────────────────────────
    @GetMapping("/admin/sponsors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Sponsor>> getAll() {
        return ResponseEntity.ok(sponsorService.getAllForAdmin());
    }

    @PostMapping("/admin/sponsors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Sponsor> create(
            @RequestParam("name")                                    String name,
            @RequestParam(value = "address",     required = false)  String address,
            @RequestParam(value = "websiteUrl",  required = false)  String websiteUrl,
            @RequestParam(value = "phone",       required = false)  String phone,
            @RequestParam(value = "description", required = false)  String description,
            @RequestParam(value = "logo",        required = false)  MultipartFile logo
    ) throws IOException {
        return ResponseEntity.ok(
                sponsorService.createSponsor(name, address, websiteUrl, phone, description, logo));
    }

    @PutMapping("/admin/sponsors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Sponsor> update(
            @PathVariable Long id,
            @RequestParam("name")                                    String name,
            @RequestParam(value = "address",     required = false)  String address,
            @RequestParam(value = "websiteUrl",  required = false)  String websiteUrl,
            @RequestParam(value = "phone",       required = false)  String phone,
            @RequestParam(value = "description", required = false)  String description,
            @RequestParam(value = "logo",        required = false)  MultipartFile logo
    ) throws IOException {
        return ResponseEntity.ok(
                sponsorService.updateSponsor(id, name, address, websiteUrl, phone, description, logo));
    }

    @PatchMapping("/admin/sponsors/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggle(@PathVariable Long id) {
        sponsorService.toggleVisibility(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/admin/sponsors/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorder(@RequestBody List<Long> orderedIds) {
        sponsorService.updateOrder(orderedIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/sponsors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sponsorService.deleteSponsor(id);
        return ResponseEntity.noContent().build();
    }
}
