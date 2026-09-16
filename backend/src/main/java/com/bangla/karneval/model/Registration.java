package com.bangla.karneval.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String primaryName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private LocalDate primaryDateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 30)
    private String phone;

    private Integer participantCount = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal calculatedAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(unique = true, length = 50)
    private String referenceCode;

    private Integer eventYear;

    private LocalDateTime registeredAt = LocalDateTime.now();

    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<AdditionalParticipant> additionalParticipants = new ArrayList<>();
}
