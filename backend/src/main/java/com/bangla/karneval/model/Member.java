package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "members")
public class Member {
    public enum Type { SINGLE, COUPLE }
    public enum Status { PENDING, APPROVED, REJECTED }
    public enum Delivery { NOT_REQUESTED, PENDING, SENT, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(unique = true, length = 40) private String membershipId;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 100) private String partnerName;
    private LocalDate partnerDateOfBirth;
    @Column(length = 500) private String partnerAddress;
    @Column(length = 30) private String partnerPhone;
    @Column(length = 150) private String partnerEmail;
    @Column(nullable = false) private LocalDate dateOfBirth;
    @Column(nullable = false, length = 500) private String address;
    @Column(nullable = false, length = 30) private String phone;
    @Column(nullable = false, length = 150) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Type membershipType;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal annualFee;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentMethod paymentMethod;
    private boolean paymentDeclared;
    private boolean paymentVerified;
    @Column(nullable = false) private LocalDateTime consentAt;
    @Column(nullable = false, length = 500) private String consentText;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.PENDING;
    private boolean listed = true;
    @Column(length = 2000) private String adminNote;
    private LocalDateTime appliedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Delivery emailDelivery = Delivery.NOT_REQUESTED;
}
