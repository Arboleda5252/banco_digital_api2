package com.banco.digital.controllers;

import com.banco.digital.dto.AuthRequest;
import com.banco.digital.dto.AuthResponse;
import com.banco.digital.dto.RegisterRequest;
import com.banco.digital.dto.ForgotPasswordRequest;
import com.banco.digital.dto.PasswordResetRequest;
import com.banco.digital.repositories.UserRepository;
import com.banco.digital.services.AuthService;
import com.banco.digital.services.FileStorageService;
import com.banco.digital.services.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public AuthController(AuthService authService, FileStorageService fileStorageService, UserRepository userRepository, EmailService emailService) {
        this.authService = authService;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestPart("data") RegisterRequest request,
            @RequestPart("dniFront") MultipartFile dniFrontFile,
            @RequestPart("dniBack") MultipartFile dniBackFile) {
        try {
            String dniFront = fileStorageService.saveFile(dniFrontFile);
            String dniBack = fileStorageService.saveFile(dniBackFile);
            
            AuthResponse response = authService.register(request, dniFront, dniBack);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.generatePasswordResetToken(request.getEmail(), emailService);
            return ResponseEntity.ok("Si el correo existe, se enviarán instrucciones.");
        } catch (Exception e) {
            // Siempre devolver OK por seguridad (evitar enumeración de usuarios)
            return ResponseEntity.ok("Si el correo existe, se enviarán instrucciones.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok("Contraseña actualizada exitosamente.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request, emailService);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody com.banco.digital.dto.MfaVerificationRequest request) {
        try {
            AuthResponse response = authService.verifyMfaAndGenerateToken(request.getEmail(), request.getCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }


    @PostMapping("/verify-identity")
    public ResponseEntity<?> verifyIdentity(
            org.springframework.security.core.Authentication authentication,
            @RequestPart("selfie") MultipartFile selfieFile) {
        if (authentication == null) return ResponseEntity.status(401).body("Sesión no válida");
        try {
            String selfiePath = fileStorageService.saveFile(selfieFile);
            com.banco.digital.models.User user = authService.verifyUser(authentication.getName(), selfiePath);
            user.setAccounts(new java.util.ArrayList<>()); // Evitar ciclos
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Sesión no válida");
        }
        return userRepository.findByEmail(authentication.getName())
                .<ResponseEntity<?>>map(user -> {
                    user.setAccounts(new java.util.ArrayList<>()); // Evitar ciclos JSON
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> ResponseEntity.status(404).body("Usuario no encontrado"));
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = userRepository.existsByEmail(email);
        return ResponseEntity.ok(java.util.Map.of("exists", exists));
    }
}
