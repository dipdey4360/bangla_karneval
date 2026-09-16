package com.bangla.karneval.controller;

import com.bangla.karneval.dto.request.PerformerRegistrationRequest;
import com.bangla.karneval.dto.request.GeneralRegistrationRequest;
import com.bangla.karneval.dto.response.ApiResponse;
import com.bangla.karneval.dto.response.RegistrationResponse;
import com.bangla.karneval.service.PerformerService;
import com.bangla.karneval.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/register")
public class RegistrationController {

    @Autowired private RegistrationService registrationService;
    @Autowired private PerformerService    performerService;

    @PostMapping("/general")
    public ResponseEntity<RegistrationResponse> registerGeneral(
            @Valid @RequestBody GeneralRegistrationRequest request) {
        return ResponseEntity.ok(registrationService.registerParticipant(request));
    }

    @PostMapping("/performer")
    public ResponseEntity<ApiResponse> registerPerformer(
            @Valid @RequestBody PerformerRegistrationRequest request) {
        performerService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Performer registration submitted successfully!"));
    }
}
