package com.banco.digital.controllers;

import com.banco.digital.dto.TransactionRequest;
import com.banco.digital.models.Transaction;
import com.banco.digital.models.User;
import com.banco.digital.repositories.UserRepository;
import com.banco.digital.services.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final com.banco.digital.services.AuthService authService;
    private final com.banco.digital.services.EmailService emailService;
    private final com.banco.digital.services.AuditService auditService;

    public TransactionController(TransactionService transactionService, UserRepository userRepository, com.banco.digital.services.AuthService authService, com.banco.digital.services.EmailService emailService, com.banco.digital.services.AuditService auditService) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(Authentication authentication) {
        try {
            authService.generateMfaCode(authentication.getName(), emailService);
            return ResponseEntity.ok("Código de seguridad enviado al correo");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransactionRequest request) {
        try {
            transactionService.transfer(
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount(),
                request.getDescription(),
                request.getMfaCode()
            );
            return ResponseEntity.ok("Transferencia exitosa");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<com.banco.digital.dto.TransactionDTO>> getHistory(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        auditService.logAction(user.getEmail(), "VIEW_HISTORY", "SUCCESS", "Consulta de historial de transacciones", "system");
        
        return ResponseEntity.ok(transactionService.getHistory(user.getId()));
    }
}
