package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "event_config")
public class EventConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer eventYear;

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerPerson = BigDecimal.valueOf(10.00);

    private LocalDate eventDate;

    @Column(length = 255)
    private String eventLocation;

    @Column(columnDefinition = "TEXT")
    private String aboutText;

    private Boolean performerEnabled = true;

    private Boolean registrationEnabled = true;

    @Column(length = 30)  private String contactPhone;
    @Column(length = 150) private String contactEmail;
    @Column(length = 30)  private String contactWhatsapp;
    @Column(length = 255) private String contactFacebook;
    @Column(length = 255) private String contactInstagram;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
