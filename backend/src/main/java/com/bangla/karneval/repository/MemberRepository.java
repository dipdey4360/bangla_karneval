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
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findLockedById(@Param("id") Long id);
}
