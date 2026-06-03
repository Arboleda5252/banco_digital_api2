package com.banco.digital.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // Canales de notificación (HU-12)
    @Column(name = "email_enabled")
    @Builder.Default
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled")
    @Builder.Default
    private boolean smsEnabled = true;

    // Tipos de notificaciones (HU-12)
    @Column(name = "notify_transfers")
    @Builder.Default
    private boolean notifyTransfers = true;

    @Column(name = "notify_deposits")
    @Builder.Default
    private boolean notifyDeposits = true;

    @Column(name = "notify_withdrawals")
    @Builder.Default
    private boolean notifyWithdrawals = true;
}
