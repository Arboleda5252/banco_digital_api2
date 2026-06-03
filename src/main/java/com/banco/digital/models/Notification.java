package com.banco.digital.models;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Tipo de movimiento: TRANSFER_SENT, TRANSFER_RECEIVED, DEPOSIT, WITHDRAWAL
    @Column(name = "movement_type", nullable = false)
    private String movementType;

    // Canal utilizado: EMAIL, SMS
    @Column(nullable = false)
    private String channel;

    // Estado de envío: DELIVERED, PENDING, ERROR
    @Column(nullable = false)
    private String status;

    private BigDecimal amount;

    // Mensaje legible enviado al usuario (HU-12: tipo, monto y fecha)
    @Column(length = 500)
    private String message;

    // Destino del envío (correo enmascarado o teléfono)
    private String destination;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
