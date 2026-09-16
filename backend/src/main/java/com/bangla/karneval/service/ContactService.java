package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.ContactInquiryRequest;
import com.bangla.karneval.model.ContactInquiry;
import com.bangla.karneval.repository.ContactInquiryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContactService {

    @Autowired private ContactInquiryRepository contactInquiryRepository;
    @Autowired private EmailService emailService;

    public ContactInquiry save(ContactInquiryRequest request) {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setName(request.getName());
        inquiry.setEmail(request.getEmail());
        inquiry.setMessage(request.getMessage());
        return contactInquiryRepository.save(inquiry);
    }

    public List<ContactInquiry> getAll() {
        return contactInquiryRepository.findAllByOrderBySubmittedAtDesc();
    }

    public long getUnreadCount() {
        return contactInquiryRepository.countByReadFalse();
    }

    public ContactInquiry markRead(Long id) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        inquiry.setRead(true);
        return contactInquiryRepository.save(inquiry);
    }

    public ContactInquiry reply(Long id, String replyText) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        inquiry.setRead(true);
        inquiry.setAnswered(true);
        inquiry.setReplyText(replyText);
        inquiry.setRepliedAt(LocalDateTime.now());
        ContactInquiry saved = contactInquiryRepository.save(inquiry);
        emailService.sendContactReply(inquiry.getName(), inquiry.getEmail(),
                inquiry.getMessage(), replyText);
        return saved;
    }
}
