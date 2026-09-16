package com.bangla.karneval.repository;

import com.bangla.karneval.model.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long> {
    List<Sponsor> findByIsVisibleTrueOrderByDisplayOrderAsc();
    List<Sponsor> findAllByOrderByDisplayOrderAsc();
}
