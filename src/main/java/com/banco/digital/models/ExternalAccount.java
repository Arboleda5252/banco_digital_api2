package com.banco.digital.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "external_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bankName; // Bancolombia, Nequi, etc.

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String holderName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;
}
