package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "application_settings")
public class ApplicationSettings {
    @Id private Integer id = 1;
    @Column(nullable = false) private Integer activeEventYear;
    @Column(columnDefinition = "TEXT") private String membershipBenefits;
    @Column(precision = 10, scale = 2) private BigDecimal membershipSingleFee;
    @Column(precision = 10, scale = 2) private BigDecimal membershipCoupleFee;
    @Column(columnDefinition = "TEXT") private String membershipPaymentInstructions;
}
