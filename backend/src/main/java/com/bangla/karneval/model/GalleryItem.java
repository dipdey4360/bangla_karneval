package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "gallery_items")
public class GalleryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer eventYear;
    private Long eventEditionId;

    @Column(length = 10)
    private String mediaType; // IMAGE, VIDEO

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 255)
    private String caption;

    private Boolean isHighlight = false;
    private Integer displayOrder = 0;
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
