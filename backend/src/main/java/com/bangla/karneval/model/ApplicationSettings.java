package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "application_settings")
public class ApplicationSettings {
    @Column(precision = 5, scale = 2) private BigDecimal memberDiscountPercent = BigDecimal.ZERO;
    @Id private Integer id = 1;
    @Column(nullable = false) private Integer activeEventYear;
    private Long activeEventEditionId;
    @Column(columnDefinition = "TEXT") private String membershipBenefits;
    @Column(precision = 10, scale = 2) private BigDecimal membershipSingleFee;
    @Column(precision = 10, scale = 2) private BigDecimal membershipCoupleFee;
    @Column(columnDefinition = "TEXT") private String membershipPaymentInstructions;
}
