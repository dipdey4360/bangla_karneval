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
    @Transient private Long eventEditionId;
    @Transient private Integer legacyEventYear;
    @Transient private String title;
    @Transient private String programmeCode;
    @Transient private String tagline;
    @Transient private String themeKey;
    @Transient private String accentColor;
    @Transient private String posterPath;
    @Transient private String paymentInstructions;
    @Transient private Long version;


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

    @Column(columnDefinition = "TEXT") private String membershipBenefits;
    @Column(precision = 10, scale = 2) private BigDecimal membershipSingleFee;
    @Column(precision = 10, scale = 2) private BigDecimal membershipCoupleFee;
    @Column(columnDefinition = "TEXT") private String membershipPaymentInstructions;

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
