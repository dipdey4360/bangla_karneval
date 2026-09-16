package com.bangla.karneval.repository;

import com.bangla.karneval.model.EventConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventConfigRepository extends JpaRepository<EventConfig, Integer> {
    Optional<EventConfig> findByEventYear(Integer eventYear);
    List<EventConfig> findAllByOrderByEventYearDesc();
}
