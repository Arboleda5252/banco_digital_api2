package com.banco.digital.services;

import com.banco.digital.dto.NotificationDTO;
import com.banco.digital.dto.NotificationPreferenceDTO;
import com.banco.digital.models.Notification;
import com.banco.digital.models.NotificationPreference;
import com.banco.digital.models.User;
import com.banco.digital.repositories.NotificationPreferenceRepository;
import com.banco.digital.repositories.NotificationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    // Tipos de movimiento soportados (HU-12)
    public static final String TRANSFER_SENT = "TRANSFER_SENT";
    public static final String TRANSFER_RECEIVED = "TRANSFER_RECEIVED";
    public static final String DEPOSIT = "DEPOSIT";
    public static final String WITHDRAWAL = "WITHDRAWAL";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationPreferenceRepository preferenceRepository,
                               NotificationRepository notificationRepository) {
        this.preferenceRepository = preferenceRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Obtiene las preferencias del usuario, creándolas con valores por defecto la primera vez.
     */
    public NotificationPreference getOrCreatePreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> preferenceRepository.save(
                        NotificationPreference.builder().userId(userId).build()));
    }

    public NotificationPreferenceDTO getPreferencesDTO(User user) {
        NotificationPreference pref = getOrCreatePreferences(user.getId());
        return toDTO(pref, user);
    }

    public NotificationPreferenceDTO updatePreferences(User user, NotificationPreferenceDTO dto) {
        NotificationPreference pref = getOrCreatePreferences(user.getId());
        pref.setEmailEnabled(dto.isEmailEnabled());
        pref.setSmsEnabled(dto.isSmsEnabled());
        pref.setNotifyTransfers(dto.isNotifyTransfers());
        pref.setNotifyDeposits(dto.isNotifyDeposits());
        pref.setNotifyWithdrawals(dto.isNotifyWithdrawals());
        preferenceRepository.save(pref);
        return toDTO(pref, user);
    }

    public List<NotificationDTO> getHistory(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> NotificationDTO.builder()
                        .id(n.getId())
                        .movementType(n.getMovementType())
                        .channel(n.getChannel())
                        .status(n.getStatus())
                        .amount(n.getAmount())
                        .message(n.getMessage())
                        .destination(n.getDestination())
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Notifica un movimiento al usuario respetando sus preferencias (HU-12).
     * Por cada canal habilitado registra la notificación y simula el envío por consola.
     * No interrumpe la transacción si algo falla.
     */
    public void notifyMovement(User user, String movementType, BigDecimal amount) {
        if (user == null) return;

        try {
            NotificationPreference pref = getOrCreatePreferences(user.getId());

            if (!isMovementEnabled(pref, movementType)) {
                return;
            }

            String message = buildMessage(movementType, amount);

            if (pref.isEmailEnabled() && user.getEmail() != null && !user.getEmail().isBlank()) {
                dispatch(user.getId(), movementType, "EMAIL", amount, message, maskEmail(user.getEmail()));
            }
            if (pref.isSmsEnabled() && user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
                dispatch(user.getId(), movementType, "SMS", amount, message, maskPhone(user.getPhoneNumber()));
            }
        } catch (Exception e) {
            // La notificación nunca debe tumbar la operación financiera.
            System.out.println("[NOTIFICACIÓN] Error al notificar movimiento " + movementType + ": " + e.getMessage());
        }
    }

    private void dispatch(Long userId, String movementType, String channel, BigDecimal amount,
                          String message, String destination) {
        Notification notification = Notification.builder()
                .userId(userId)
                .movementType(movementType)
                .channel(channel)
                .status("DELIVERED")
                .amount(amount)
                .message(message)
                .destination(destination)
                .build();
        notificationRepository.save(notification);

        // Envío simulado (consistente con EmailService)
        System.out.println("================================");
        System.out.println("SIMULACIÓN DE NOTIFICACIÓN (" + channel + ")");
        System.out.println("Para: " + destination);
        System.out.println("Mensaje: " + message);
        System.out.println("================================");
    }

    private boolean isMovementEnabled(NotificationPreference pref, String movementType) {
        switch (movementType) {
            case TRANSFER_SENT:
            case TRANSFER_RECEIVED:
                return pref.isNotifyTransfers();
            case DEPOSIT:
                return pref.isNotifyDeposits();
            case WITHDRAWAL:
                return pref.isNotifyWithdrawals();
            default:
                return false;
        }
    }

    private String buildMessage(String movementType, BigDecimal amount) {
        String fecha = LocalDateTime.now().format(DATE_FORMAT);
        String monto = formatMoney(amount);
        switch (movementType) {
            case TRANSFER_SENT:
                return "Transferencia enviada por " + monto + " el " + fecha + ".";
            case TRANSFER_RECEIVED:
                return "Transferencia recibida por " + monto + " el " + fecha + ".";
            case DEPOSIT:
                return "Depósito por " + monto + " realizado el " + fecha + ".";
            case WITHDRAWAL:
                return "Retiro por " + monto + " realizado el " + fecha + ".";
            default:
                return "Movimiento por " + monto + " el " + fecha + ".";
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "$0";
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }

    private NotificationPreferenceDTO toDTO(NotificationPreference pref, User user) {
        return NotificationPreferenceDTO.builder()
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .emailEnabled(pref.isEmailEnabled())
                .smsEnabled(pref.isSmsEnabled())
                .notifyTransfers(pref.isNotifyTransfers())
                .notifyDeposits(pref.isNotifyDeposits())
                .notifyWithdrawals(pref.isNotifyWithdrawals())
                .build();
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String name = email.substring(0, at);
        String visible = name.substring(0, Math.min(2, name.length()));
        return visible + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone.length() < 4) return "***";
        return "*** *** " + phone.substring(phone.length() - 4);
    }
}
