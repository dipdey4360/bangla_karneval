package com.bangla.karneval.controller;

import com.bangla.karneval.dto.request.ContactInquiryRequest;
import com.bangla.karneval.dto.response.ApiResponse;
import com.bangla.karneval.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Autowired private ContactService contactService;

    @PostMapping
    public ResponseEntity<ApiResponse> submitContact(
            @Valid @RequestBody ContactInquiryRequest request) {
        contactService.save(request);
        return ResponseEntity.ok(ApiResponse.ok("Your message has been received. We'll be in touch soon!"));
    }
    @GetMapping("/admin/messages")
    public ResponseEntity<?> getAllMessages() {
        return ResponseEntity.ok(contactService.getAll());
    }

    @GetMapping("/admin/messages/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", contactService.getUnreadCount()));
    }

    @PatchMapping("/admin/messages/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.markRead(id));
    }

    @PostMapping("/admin/messages/{id}/reply")
    public ResponseEntity<?> reply(@PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(contactService.reply(id, body.get("replyText")));
    }

}
