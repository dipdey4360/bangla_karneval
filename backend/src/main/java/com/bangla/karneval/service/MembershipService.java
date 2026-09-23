package com.bangla.karneval.service;

import com.bangla.karneval.dto.request.*;
import com.bangla.karneval.model.Member;
import com.bangla.karneval.repository.MemberRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MembershipService {
    public static final String CONSENT = "I consent to the storage and use of the submitted personal data to manage my membership. Public display of members’ names follows my visibility preference.";
    public record DecisionEmail(Long memberId) {}
    public record PublicMember(String name) {}
    private final MemberRepository repository;
    private final ApplicationSettingsService configService;
    private final ApplicationEventPublisher events;
    public MembershipService(MemberRepository repository, ApplicationSettingsService configService, ApplicationEventPublisher events) {
        this.repository = repository; this.configService = configService; this.events = events;
    }
    public MembershipSettingsRequest settings() {
        var c = configService.getSettings();
        return new MembershipSettingsRequest(
            c.getMembershipBenefits() == null ? "Voting rights\nEligibility to become a board member\nDiscounts at all events" : c.getMembershipBenefits(),
            c.getMembershipSingleFee() == null ? new BigDecimal("25.00") : c.getMembershipSingleFee(),
            c.getMembershipCoupleFee() == null ? new BigDecimal("30.00") : c.getMembershipCoupleFee(),
            c.getMembershipPaymentInstructions() == null ? "Please contact the organisers for bank transfer or PayPal details before donating. Include your name and ‘Membership’ as the donation reference." : c.getMembershipPaymentInstructions(), c.getMemberDiscountPercent() == null ? BigDecimal.ZERO : c.getMemberDiscountPercent());
    }
    @Transactional
    public MembershipSettingsRequest updateSettings(MembershipSettingsRequest request) {
        var c = configService.getSettings();
        c.setMembershipBenefits(request.benefits().trim());
        c.setMembershipSingleFee(request.singleFee()); c.setMembershipCoupleFee(request.coupleFee());
        c.setMembershipPaymentInstructions(request.paymentInstructions().trim());
        if (request.memberDiscountPercent() != null) c.setMemberDiscountPercent(request.memberDiscountPercent());
        return settings();
    }
    @Transactional
    public Member apply(MembershipRequest request) {
        if (request.listed() == null)
            throw new ResponseStatusException(BAD_REQUEST, "Please choose whether to display your name publicly");
        if (!Boolean.TRUE.equals(request.consent()) || !Boolean.TRUE.equals(request.paymentDeclared()))
            throw new ResponseStatusException(BAD_REQUEST, "Consent and donation declaration are required");
        if (request.membershipType() == Member.Type.COUPLE && (request.partnerName() == null || request.partnerName().isBlank()))
            throw new ResponseStatusException(BAD_REQUEST, "Please enter your partner's name for couple membership");
        if (request.membershipType() == Member.Type.COUPLE && (request.partnerDateOfBirth() == null
                || request.partnerPhone() == null || request.partnerPhone().isBlank()
                || request.partnerEmail() == null || request.partnerEmail().isBlank()))
            throw new ResponseStatusException(BAD_REQUEST, "Please complete your partner's date of birth, phone and email");
        var prices = settings();
        Member m = new Member();
        m.setName(request.name().trim());
        m.setPartnerName(request.membershipType() == Member.Type.COUPLE ? request.partnerName().trim() : null);
        if (request.membershipType() == Member.Type.COUPLE) {
            m.setPartnerDateOfBirth(request.partnerDateOfBirth());
            m.setPartnerAddress(null);
            m.setPartnerPhone(request.partnerPhone().trim());
            m.setPartnerEmail(request.partnerEmail().trim().toLowerCase(Locale.ROOT));
        }
        m.setDateOfBirth(request.dateOfBirth()); m.setAddress(request.address().trim());
        m.setPhone(request.phone().trim()); m.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        m.setMembershipType(request.membershipType());
        m.setAnnualFee(request.membershipType() == Member.Type.COUPLE ? prices.coupleFee() : prices.singleFee());
        m.setPaymentMethod(request.paymentMethod()); m.setPaymentDeclared(true);
        m.setListed(request.listed());
        m.setConsentAt(LocalDateTime.now()); m.setConsentText(CONSENT);
        return repository.save(m);
    }
    public List<Member> applications() { return repository.findAllByOrderByAppliedAtDesc(); }
    public List<PublicMember> publicMembers() {
        List<PublicMember> names = new ArrayList<>();
        for (Member m : repository.findByStatusAndListedTrueOrderByNameAsc(Member.Status.APPROVED)) {
            names.add(new PublicMember(m.getName()));
            if (m.getMembershipType() == Member.Type.COUPLE && m.getPartnerName() != null && !m.getPartnerName().isBlank())
                names.add(new PublicMember(m.getPartnerName()));
        }
        return names;
    }
    @Transactional
    public Member decide(Long id, MembershipDecisionRequest request) {
        if (request.status() == Member.Status.PENDING) throw new ResponseStatusException(BAD_REQUEST, "Choose approve or reject");
        Member m = locked(id);
        if (m.getStatus() == request.status()) return m; // Repeated clicks do not send duplicate emails.
        if (request.status() == Member.Status.APPROVED && !request.paymentVerified())
            throw new ResponseStatusException(BAD_REQUEST, "Verify the donation before approving membership");
        if (request.status() == Member.Status.APPROVED && m.getMembershipId() == null)
            m.setMembershipId(String.format(Locale.ROOT, "BKM-%05d", repository.nextMembershipNumber()));
        m.setStatus(request.status()); m.setPaymentVerified(request.paymentVerified());
        m.setAdminNote(request.adminNote()); m.setReviewedAt(LocalDateTime.now());
        m.setEmailDelivery(Member.Delivery.PENDING);
        events.publishEvent(new DecisionEmail(id));
        return repository.save(m);
    }
    @Transactional
    public void retryEmail(Long id) {
        Member m = locked(id);
        if (m.getStatus() == Member.Status.PENDING || m.getEmailDelivery() == Member.Delivery.SENT)
            throw new ResponseStatusException(BAD_REQUEST, "There is no failed decision email to retry");
        m.setEmailDelivery(Member.Delivery.PENDING);
        events.publishEvent(new DecisionEmail(id));
    }
    @Transactional
    public void delete(Long id) {
        repository.delete(locked(id));
    }
    private Member locked(Long id) {
        return repository.findLockedById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Membership application not found"));
    }
}
