package com.bangla.karneval.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "board_members")
public class BoardMember {
    // Six fixed positions keep the public board in a predictable order.
    @Id private Integer id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 100) private String designation;
    @Column(length = 500) private String imageUrl;
}
