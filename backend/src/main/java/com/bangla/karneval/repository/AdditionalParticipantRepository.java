package com.bangla.karneval.repository;

import com.bangla.karneval.model.AdditionalParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdditionalParticipantRepository extends JpaRepository<AdditionalParticipant, Long> {
    List<AdditionalParticipant> findByRegistrationId(Long registrationId);
}
