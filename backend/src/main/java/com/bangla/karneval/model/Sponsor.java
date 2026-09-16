package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "sponsors")
public class Sponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String websiteUrl;

    @Column(length = 30)
    private String phone;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String logoPath;

    private Boolean isVisible = true;

    private Integer displayOrder = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
}
