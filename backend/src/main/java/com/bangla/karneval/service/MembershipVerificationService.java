package com.bangla.karneval.service;

import com.bangla.karneval.model.Member;
import com.bangla.karneval.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class MembershipVerificationService {
    private final MemberRepository members;
    private final ApplicationSettingsService settings;
    public MembershipVerificationService(MemberRepository members, ApplicationSettingsService settings) {
        this.members = members; this.settings = settings;
    }
    public record Verification(boolean verified, BigDecimal discountPercent) {}
    public static String normalizeName(String name) {
        return Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFKC)
            .strip().replaceAll("(?U)\\s+", " ").toLowerCase(Locale.ROOT);
    }
    @Transactional
    public Verification verify(String membershipId, String name) {
        String id = membershipId == null ? "" : membershipId.strip().toUpperCase(Locale.ROOT);
        Member m = members.findByMembershipId(id).orElseThrow(MembershipVerificationService::invalid);
        String normalized = normalizeName(name);
        boolean matches = !normalized.isEmpty() && (normalized.equals(normalizeName(m.getName()))
            || (m.getMembershipType() == Member.Type.COUPLE && normalized.equals(normalizeName(m.getPartnerName()))));
        if (m.getStatus() != Member.Status.APPROVED || !m.isPaymentVerified() || !matches) throw invalid();
        BigDecimal percent = settings.getSettings().getMemberDiscountPercent();
        if (percent == null) percent = BigDecimal.ZERO;
        return new Verification(true, percent);
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(BAD_REQUEST, "Membership could not be verified. Check your membership ID and full name.");
    }
}
