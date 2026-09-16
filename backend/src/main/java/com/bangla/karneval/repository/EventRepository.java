package com.bangla.karneval.repository;

import com.bangla.karneval.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByEventYear(Integer year);
    List<Event> findByEventYearAndIsHighlight(Integer year, Boolean isHighlight);
}
