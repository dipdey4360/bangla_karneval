package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "performer_group_members")
public class PerformerGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performer_registration_id", nullable = false)
    private PerformerRegistration performerRegistration;

    @Column(nullable = false, length = 100)
    private String name;

    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;
}

