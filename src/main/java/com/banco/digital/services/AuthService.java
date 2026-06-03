package com.banco.digital.services;

import com.banco.digital.dto.AuthRequest;
import com.banco.digital.dto.AuthResponse;
import com.banco.digital.dto.RegisterRequest;
import com.banco.digital.models.Account;
import com.banco.digital.models.User;
import com.banco.digital.repositories.AccountRepository;
import com.banco.digital.repositories.UserRepository;
import com.banco.digital.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;
    private final com.banco.digital.repositories.VerifiedIdentityRepository verifiedIdentityRepository;

    public AuthService(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, AuditService auditService, com.banco.digital.repositories.VerifiedIdentityRepository verifiedIdentityRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.auditService = auditService;
        this.verifiedIdentityRepository = verifiedIdentityRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String dniFront, String dniBack) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El correo ya está registrado");
        }

        if (userRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new RuntimeException("Este número de documento ya está vinculado a una cuenta");
        }

        // Validación KYC Local (Fase 5)
        com.banco.digital.models.VerifiedIdentity identity = verifiedIdentityRepository.findByDocumentNumber(request.getDocumentNumber())
                .orElseThrow(() -> new RuntimeException("Este documento no es válido porque no se encuentra registrado en el sistema de registraduría"));
        
        if (identity.isUsed()) {
            throw new RuntimeException("Documento ya vinculado a otra cuenta");
        }

        validatePasswordStrength(request.getPassword());

        // Marcar identidad como usada inmediatamente (Fase 10)
        identity.setUsed(true);
        verifiedIdentityRepository.save(identity);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .documentNumber(request.getDocumentNumber())
                .expeditionDate(request.getExpeditionDate())
                .expeditionPlace(request.getExpeditionPlace())
                .phoneNumber(request.getPhoneNumber())
                .dniFront(dniFront)
                .dniBack(dniBack)
                .isVerified(false)
                .build();

        user = userRepository.save(user);

        // Generar números de cuenta aleatorios de 16 dígitos
        String checkingNumber = java.util.stream.IntStream.range(0, 16)
                .mapToObj(i -> String.valueOf((int) (Math.random() * 10)))
                .reduce("", String::concat);
        
        String savingsNumber = java.util.stream.IntStream.range(0, 16)
                .mapToObj(i -> String.valueOf((int) (Math.random() * 10)))
                .reduce("", String::concat);

        // Crear cuentas por defecto
        Account checking = Account.builder()
                .type("CHECKING")
                .accountNumber(checkingNumber)
                .balance(java.math.BigDecimal.ZERO)
                .user(user)
                .build();

        Account savings = Account.builder()
                .type("SAVINGS")
                .accountNumber(savingsNumber)
                .balance(java.math.BigDecimal.ZERO)
                .user(user)
                .build();

        accountRepository.save(checking);
        accountRepository.save(savings);

        String token = jwtUtils.generateToken(user.getEmail());
        
        user.setAccounts(new ArrayList<>()); // Evitar ciclo infinito JSON si se lazy load
        return new AuthResponse(token, user);
    }

    @Transactional
    public User verifyUser(String email, String selfiePath) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setSelfie(selfiePath);
        user.setVerified(true);
        
        auditService.logAction(email, "IDENTITY_VERIFIED", "SUCCESS", "KYC completado con éxito", "system");
        
        return userRepository.save(user);
    }

    public AuthResponse login(AuthRequest request, EmailService emailService) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!user.isAccountNonLocked()) {
            if (user.getLockTime() != null && user.getLockTime().plusMinutes(15).isBefore(java.time.LocalDateTime.now())) {
                // Auto-desbloqueo tras 15 minutos
                user.setAccountNonLocked(true);
                user.setFailedAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            } else {
                long secondsElapsed = java.time.Duration.between(user.getLockTime(), java.time.LocalDateTime.now()).getSeconds();
                long remainingSeconds = Math.max(0, 900 - secondsElapsed); // 900s = 15min
                throw new RuntimeException("ACCOUNT_LOCKED:" + remainingSeconds);
            }
        }

        if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.setFailedAttempts(0);
            userRepository.save(user);

            // Generar y enviar código MFA (OTP)
            generateMfaCode(user.getEmail(), emailService);
            
            auditService.logAction(user.getEmail(), "LOGIN_STEP_1_SUCCESS", "SUCCESS", "Contraseña correcta, se requiere OTP", "system");
            
            // Retornamos una señal que el controlador capturará
            throw new RuntimeException("OTP_REQUIRED");
        } else {
            // Incrementar intentos fallidos...
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            
            if (attempts >= 3) {
                user.setAccountNonLocked(false);
                user.setLockTime(java.time.LocalDateTime.now());
                userRepository.save(user);
                auditService.logAction(request.getEmail(), "ACCOUNT_LOCKED", "SECURITY", "Cuenta bloqueada por intentos fallidos", "system");
                throw new RuntimeException("ACCOUNT_LOCKED:900");
            }
            userRepository.save(user);
            auditService.logAction(request.getEmail(), "LOGIN_FAILED", "FAILED", "Credenciales incorrectas", "system");
            throw new RuntimeException("Credenciales inválidas");
        }
    }
    
    @Transactional
    public void generatePasswordResetToken(String email, EmailService emailService) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        String token = java.util.UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(30));
        
        userRepository.save(user);
        
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o no encontrado"));
                
        if (user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("El token ha expirado");
        }
        
        validatePasswordStrength(newPassword);
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        
        userRepository.save(user);
    }

    @Transactional
    public void generateMfaCode(String email, EmailService emailService) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        user.setMfaCode(code);
        user.setMfaExpiry(java.time.LocalDateTime.now().plusMinutes(5));
        
        userRepository.save(user);
        
        emailService.sendMfaEmail(user.getEmail(), code);
        auditService.logAction(user.getEmail(), "MFA_CODE_SENT", "SUCCESS", "Código MFA enviado al correo", "system");
    }

    @Transactional
    public boolean verifyMfaCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        if (user.getMfaCode() == null || !user.getMfaCode().equals(code)) {
            auditService.logAction(email, "MFA_VERIFY_FAILED", "FAILED", "Código MFA incorrecto", "system");
            return false;
        }
        
        if (user.getMfaExpiry().isBefore(java.time.LocalDateTime.now())) {
            auditService.logAction(email, "MFA_VERIFY_FAILED", "FAILED", "Código MFA expirado", "system");
            return false;
        }
        
        // Limpiar el código tras uso exitoso
        user.setMfaCode(null);
        user.setMfaExpiry(null);
        userRepository.save(user);
        
        auditService.logAction(email, "MFA_VERIFY_SUCCESS", "SUCCESS", "MFA verificado correctamente", "system");
        return true;
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("La contraseña debe tener al menos 8 caracteres");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("La contraseña debe tener al menos una letra mayúscula");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new RuntimeException("La contraseña debe tener al menos un número");
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            throw new RuntimeException("La contraseña debe tener al menos un carácter especial");
        }
    }

    @Transactional
    public AuthResponse verifyMfaAndGenerateToken(String email, String code) {
        if (!verifyMfaCode(email, code)) {
            throw new RuntimeException("Código MFA inválido o expirado");
        }
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        String token = jwtUtils.generateToken(user.getEmail());
        user.setAccounts(new ArrayList<>()); // Evitar ciclos
        
        auditService.logAction(email, "LOGIN_SUCCESS", "SUCCESS", "MFA verificado, sesión iniciada", "system");
        
        return new AuthResponse(token, user, false);
    }
}
