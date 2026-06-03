package com.banco.digital.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String action; // Ej: LOGIN_SUCCESS, TRANSFER_FAILED, ACCOUNT_LOCKED

    private String email; // Usuario involucrado

    private String details; // Descripción adicional

    private String ipAddress;

    private String status; // SUCCESS, FAILED, WARNING
}
