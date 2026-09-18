package com.bangla.karneval.repository;

import com.bangla.karneval.model.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GalleryItemRepository extends JpaRepository<GalleryItem, Long> {
    java.util.List<GalleryItem> findByEventEditionIdOrderByDisplayOrderAsc(Long eventEditionId);
    List<GalleryItem> findByEventYearOrderByDisplayOrderAsc(Integer year);
    List<GalleryItem> findByEventYearAndIsHighlight(Integer year, Boolean isHighlight);

    @Query("SELECT DISTINCT g.eventYear FROM GalleryItem g ORDER BY g.eventYear DESC")
    List<Integer> findDistinctEventYears();
}
