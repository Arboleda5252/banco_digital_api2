package com.banco.digital.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDTO {
    // Datos de contacto mostrados en los canales (HU-12)
    private String email;
    private String phoneNumber;

    // Canales
    private boolean emailEnabled;
    private boolean smsEnabled;

    // Tipos de notificaciones
    private boolean notifyTransfers;
    private boolean notifyDeposits;
    private boolean notifyWithdrawals;
}
