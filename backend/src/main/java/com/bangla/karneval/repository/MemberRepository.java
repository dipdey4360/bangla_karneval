package com.bangla.karneval.repository;
import com.bangla.karneval.model.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findAllByOrderByAppliedAtDesc();
    List<Member> findByStatusAndListedTrueOrderByNameAsc(Member.Status status);
    Optional<Member> findByMembershipId(String membershipId);
    @Query(value = "SELECT nextval('membership_number_seq')", nativeQuery = true)
    Long nextMembershipNumber();
    @Query("""
        select m.id from Member m where m.status = :status and m.paymentVerified = true
          and m.membershipExpiresOn > :today and m.membershipExpiresOn <= :windowEnd
          and (m.expiryReminderSentAt is null or
               (m.membershipType = :coupleType and m.partnerExpiryReminderSentAt is null))
        """)
    List<Long> findExpiryReminderCandidates(@Param("status") Member.Status status,
        @Param("coupleType") Member.Type coupleType, @Param("today") java.time.LocalDate today,
        @Param("windowEnd") java.time.LocalDate windowEnd);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findLockedById(@Param("id") Long id);
}
