package com.banco.digital.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "verified_identities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiedIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String documentNumber; // DNI o Cédula

    @Column(nullable = false)
    private String fullName;

    private String expeditionDate;
    private String expeditionPlace;

    @Builder.Default
    private boolean isUsed = false;
}
