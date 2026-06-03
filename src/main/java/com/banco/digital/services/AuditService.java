package com.banco.digital.services;

import com.banco.digital.models.AuditLog;
import com.banco.digital.repositories.AuditLogRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(String email, String action, String status, String details, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .email(email)
                .action(action)
                .status(status)
                .details(details)
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }
}
