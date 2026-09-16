package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "contact_inquiries")
public class ContactInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 150) private String email;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private boolean read      = false;
    private boolean answered  = false;

    @Column(columnDefinition = "TEXT")
    private String replyText;

    private LocalDateTime repliedAt;

    private LocalDateTime submittedAt = LocalDateTime.now();
}
