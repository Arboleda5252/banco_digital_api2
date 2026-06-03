package com.banco.digital.controllers;

import com.banco.digital.dto.NotificationDTO;
import com.banco.digital.dto.NotificationPreferenceDTO;
import com.banco.digital.models.User;
import com.banco.digital.repositories.UserRepository;
import com.banco.digital.services.AuditService;
import com.banco.digital.services.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository,
                                  AuditService auditService) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDTO> getPreferences(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(notificationService.getPreferencesDTO(user));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDTO> updatePreferences(Authentication authentication,
                                                                       @RequestBody NotificationPreferenceDTO request) {
        User user = currentUser(authentication);
        NotificationPreferenceDTO updated = notificationService.updatePreferences(user, request);
        auditService.logAction(user.getEmail(), "UPDATE_NOTIFICATION_PREFERENCES", "SUCCESS",
                "Actualización de preferencias de notificación", "system");
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/history")
    public ResponseEntity<List<NotificationDTO>> getHistory(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(notificationService.getHistory(user.getId()));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
