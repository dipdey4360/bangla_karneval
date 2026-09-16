package com.bangla.karneval.repository;

import com.bangla.karneval.model.PerformerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformerRegistrationRepository extends JpaRepository<PerformerRegistration, Long> {
    List<PerformerRegistration> findByEventYear(Integer year);
    List<PerformerRegistration> findByApprovalStatus(String status);
}
