package com.bangla.karneval.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer eventYear;

    @Column(length = 50)
    private String category; // DANCE, DRAMA, FOOD, CONCERT, GAMES, DRESS

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JsonProperty("isHighlight")
    private Boolean isHighlight = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
