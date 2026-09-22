package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "performer_registrations")
public class PerformerRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 150) private String email;
    @Column(length = 30)  private String phone;

    private LocalDate dateOfBirth;                 // ← changed from age

    @Column(columnDefinition = "TEXT") private String address;

    @Column(length = 100)
    private String performanceType;

    @Column(columnDefinition = "TEXT")
    private String performanceDescription;

    private Integer groupMemberCount = 1;

    @Column(length = 20)
    private String approvalStatus = "PENDING";

    private Integer eventYear;
    private Long eventEditionId;
    private String eventTitle;
    private java.time.LocalDate eventDateSnapshot;
    private String eventLocationSnapshot;
    @jakarta.persistence.Column(columnDefinition="TEXT") private String paymentInstructionsSnapshot;

    private LocalDateTime registeredAt = LocalDateTime.now();

    // Nullable for applications submitted before consent recording was introduced.
    private LocalDateTime consentAt;
    @Column(length = 500)
    private String consentText;
}
