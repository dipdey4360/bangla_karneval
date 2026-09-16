package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.PerformerRegistrationRequest;
import com.bangla.karneval.model.PerformerRegistration;
import com.bangla.karneval.repository.PerformerRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bangla.karneval.model.PerformerGroupMember;
import com.bangla.karneval.repository.PerformerGroupMemberRepository;

import java.util.List;

@Service
public class PerformerService {

    @Autowired private PerformerRegistrationRepository performerRepository;
    @Autowired private PerformerGroupMemberRepository  groupMemberRepository;
    @Autowired private EmailService emailService;

    @Transactional
    public PerformerRegistration register(PerformerRegistrationRequest request) {
        PerformerRegistration performer = new PerformerRegistration();
        performer.setName(request.getName());
        performer.setEmail(request.getEmail());
        performer.setPhone(request.getPhone());
        performer.setDateOfBirth(request.getDateOfBirth());                  // ← DOB
        performer.setAddress(request.getAddress());
        performer.setPerformanceType(request.getPerformanceType());
        performer.setPerformanceDescription(request.getPerformanceDescription());
        performer.setGroupMemberCount(request.getGroupMemberCount());
        performer.setApprovalStatus("PENDING");
        performer.setEventYear(2026);

        performer = performerRepository.save(performer);

        // Save each group member
        if (request.getGroupMembers() != null) {
            for (var m : request.getGroupMembers()) {
                PerformerGroupMember member = new PerformerGroupMember();
                member.setPerformerRegistration(performer);
                member.setName(m.getName());
                member.setDateOfBirth(m.getDateOfBirth());
                member.setGender(m.getGender());
                groupMemberRepository.save(member);
            }
        }
        return performer;
    }

    public List<PerformerRegistration> getAll(){
        return performerRepository.findAll();
    }

    public List<PerformerRegistration> getByStatus(String status)  {
        return performerRepository.findByApprovalStatus(status);
    }

    @Transactional
    public PerformerRegistration updateStatus(Long id, String status, String adminNote) {
        PerformerRegistration p = performerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Performer not found"));
        p.setApprovalStatus(status);
        PerformerRegistration saved = performerRepository.save(p);
        emailService.sendPerformerStatusEmail(saved.getEmail(), saved.getName(), status, adminNote);
        return saved;
    }

    @Transactional
    public void delete(Long id) { performerRepository.deleteById(id); }
}
