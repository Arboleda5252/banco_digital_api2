package com.banco.digital.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String documentNumber;

    private String expeditionDate;
    private String expeditionPlace;
    private String phoneNumber;

    private String dniFront;
    private String dniBack;
    private String selfie;
    @Builder.Default
    private boolean isVerified = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private java.time.LocalDateTime resetTokenExpiry;

    // Campos de Seguridad (HU-06)
    @Column(name = "failed_attempts")
    @Builder.Default
    private int failedAttempts = 0;

    @Column(name = "account_non_locked")
    @Builder.Default
    private boolean accountNonLocked = true;

    @Column(name = "lock_time")
    private java.time.LocalDateTime lockTime;

    // Campos de MFA (HU-06, HU-09)
    @Column(name = "mfa_code")
    private String mfaCode;

    @Column(name = "mfa_expiry")
    private java.time.LocalDateTime mfaExpiry;

    @Column(name = "is_mfa_enabled")
    @Builder.Default
    private boolean isMfaEnabled = false; 
}
