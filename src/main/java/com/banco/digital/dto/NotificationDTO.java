package com.banco.digital.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String movementType; // TRANSFER_SENT, TRANSFER_RECEIVED, DEPOSIT, WITHDRAWAL
    private String channel;       // EMAIL, SMS
    private String status;        // DELIVERED, PENDING, ERROR
    private BigDecimal amount;
    private String message;
    private String destination;
    private LocalDateTime createdAt;
}
