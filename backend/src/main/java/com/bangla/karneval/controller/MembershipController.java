package com.bangla.karneval.controller;

import com.bangla.karneval.dto.request.*;
import com.bangla.karneval.model.*;
import com.bangla.karneval.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

@RestController
public class MembershipController {
    private final MembershipService memberships;
    private final BoardMemberService board;
    public MembershipController(MembershipService memberships, BoardMemberService board) {
        this.memberships = memberships; this.board = board;
    }
    @GetMapping("/api/board-members") public List<BoardMember> board() { return board.list(); }
    @GetMapping("/api/members") public List<MembershipService.PublicMember> members() { return memberships.publicMembers(); }
    @GetMapping("/api/membership/settings") public MembershipSettingsRequest settings() { return memberships.settings(); }
    @PostMapping("/api/membership/applications")
    public ResponseEntity<Map<String, Object>> apply(@Valid @RequestBody MembershipRequest request) {
        Member m = memberships.apply(request);
        return ResponseEntity.status(201).body(Map.of("id", m.getId(), "annualFee", m.getAnnualFee(),
            "message", "Application received. We will email you after the admin review."));
    }
    @PutMapping("/api/admin/board-members/{slot}")
    public BoardMember saveBoard(@PathVariable int slot, @RequestParam String name,
            @RequestParam String designation, @RequestParam(required = false) MultipartFile image,
            @RequestParam(defaultValue = "false") boolean removeImage) throws IOException {
        return board.save(slot, name, designation, image, removeImage);
    }
    @DeleteMapping("/api/admin/board-members/{slot}")
    public ResponseEntity<Void> clearBoard(@PathVariable int slot) { board.clear(slot); return ResponseEntity.noContent().build(); }
    @GetMapping("/api/admin/members") public List<Member> applications() { return memberships.applications(); }
    @DeleteMapping("/api/admin/members/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberships.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/api/admin/members/{id}/decision")
    public Member decision(@PathVariable Long id, @Valid @RequestBody MembershipDecisionRequest request) { return memberships.decide(id, request); }
    @PostMapping("/api/admin/members/{id}/retry-email")
    public Map<String, String> retry(@PathVariable Long id) { memberships.retryEmail(id); return Map.of("message", "Email retry requested"); }
    @PutMapping("/api/admin/membership/settings")
    public MembershipSettingsRequest settings(@Valid @RequestBody MembershipSettingsRequest request) { return memberships.updateSettings(request); }
}
