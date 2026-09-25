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
    private final com.bangla.karneval.repository.BoardMemberRepository board;
    public MembershipVerificationService(MemberRepository members, ApplicationSettingsService settings,
            com.bangla.karneval.repository.BoardMemberRepository board) {
        this.members = members; this.settings = settings; this.board = board;
    }
    public record Verification(boolean verified, BigDecimal discountPercent) {}
    public static String normalizeName(String name) {
        return Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFKC)
            .strip().replaceAll("(?U)\\s+", " ").toLowerCase(Locale.ROOT);
    }
    @Transactional
    public Verification verify(String membershipId, String name) {
        String id = membershipId == null ? "" : membershipId.strip().toUpperCase(Locale.ROOT);
        String normalized = normalizeName(name);
        // Reserved IDs belong to positions, not to the previous occupants or their partners.
        if (id.matches("BKM-0000[1-6]")) {
            var occupant = board.findById(Integer.parseInt(id.substring(4))).orElseThrow(MembershipVerificationService::invalid);
            if (normalized.isEmpty() || !normalized.equals(normalizeName(occupant.getName()))) throw invalid();
            return discount();
        }
        Member m = members.findByMembershipId(id).orElseThrow(MembershipVerificationService::invalid);
        boolean matches = !normalized.isEmpty() && (normalized.equals(normalizeName(m.getName()))
            || (m.getMembershipType() == Member.Type.COUPLE && normalized.equals(normalizeName(m.getPartnerName()))));
        if (!m.isActiveOn(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Berlin"))) || !matches) throw invalid();
        return discount();
    }
    private Verification discount() {
        BigDecimal percent = settings.getSettings().getMemberDiscountPercent();
        if (percent == null) percent = BigDecimal.ZERO;
        return new Verification(true, percent);
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(BAD_REQUEST, "Membership could not be verified. Check your membership ID and full name.");
    }
}
