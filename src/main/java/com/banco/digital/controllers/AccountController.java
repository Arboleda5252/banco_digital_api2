package com.banco.digital.controllers;

import com.banco.digital.models.Account;
import com.banco.digital.models.User;
import com.banco.digital.repositories.AccountRepository;
import com.banco.digital.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final com.banco.digital.repositories.TransactionRepository transactionRepository;
    private final com.banco.digital.services.AuditService auditService;
    private final com.banco.digital.services.NotificationService notificationService;

    public AccountController(AccountRepository accountRepository,
                             UserRepository userRepository,
                             com.banco.digital.repositories.TransactionRepository transactionRepository,
                             com.banco.digital.services.AuditService auditService,
                             com.banco.digital.services.NotificationService notificationService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<Account>> getMyAccounts(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(accountRepository.findByUserId(user.getId()));
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> request) {
        // Validar que venga el número de cuenta
        String accountNumber = (String) request.get("accountNumber");
        if (accountNumber == null || accountNumber.isBlank()) {
            return ResponseEntity.badRequest().body("Debes indicar el número de cuenta.");
        }

        // Validar que el monto sea un número válido (HU-11)
        Object rawAmount = request.get("amount");
        if (rawAmount == null) {
            return ResponseEntity.badRequest().body("Debes indicar el monto a depositar.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(rawAmount.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("El monto ingresado no es un número válido.");
        }

        // Regla HU-11: el monto debe ser mayor a cero
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            auditService.logAction("system", "DEPOSIT_FAILED", "FAILED", "Intento de depósito con monto inválido (" + amount + ") en cuenta " + accountNumber, "system");
            return ResponseEntity.badRequest().body("El monto a depositar debe ser mayor a cero.");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (account == null) {
            auditService.logAction("system", "DEPOSIT_FAILED", "FAILED", "Intento de depósito en cuenta inexistente " + accountNumber, "system");
            return ResponseEntity.status(404).body("Cuenta " + accountNumber + " no encontrada en el sistema.");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // Registrar el movimiento en el historial
        com.banco.digital.models.Transaction transaction = com.banco.digital.models.Transaction.builder()
                .toAccountId(account.getId())
                .amount(amount)
                .type("DEPOSIT")
                .description("Abono de Capital (Terminal)")
                .build();
        transactionRepository.save(transaction);

        auditService.logAction("system", "DEPOSIT", "SUCCESS", "Depósito de " + amount + " en cuenta " + accountNumber, "system");

        // Notificar al titular de la cuenta (HU-12)
        notificationService.notifyMovement(account.getUser(), com.banco.digital.services.NotificationService.DEPOSIT, amount);

        return ResponseEntity.ok("Depósito de " + amount + " realizado con éxito en cuenta " + accountNumber);
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(Authentication authentication, @RequestBody Map<String, Object> request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validar que venga el número de cuenta
        String accountNumber = (String) request.get("accountNumber");
        if (accountNumber == null || accountNumber.isBlank()) {
            return ResponseEntity.badRequest().body("Debes indicar el número de cuenta.");
        }

        // Validar que el monto sea un número válido (HU-13)
        Object rawAmount = request.get("amount");
        if (rawAmount == null) {
            return ResponseEntity.badRequest().body("Debes indicar el monto a retirar.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(rawAmount.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("El monto ingresado no es un número válido.");
        }

        // Regla HU-13: el monto debe ser mayor a cero (impedir montos inválidos)
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            auditService.logAction(user.getEmail(), "WITHDRAWAL_FAILED", "FAILED", "Intento de retiro con monto inválido (" + amount + ") en cuenta " + accountNumber, "system");
            return ResponseEntity.badRequest().body("El monto a retirar debe ser mayor a cero.");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (account == null) {
            auditService.logAction(user.getEmail(), "WITHDRAWAL_FAILED", "FAILED", "Intento de retiro en cuenta inexistente " + accountNumber, "system");
            return ResponseEntity.status(404).body("Cuenta " + accountNumber + " no encontrada en el sistema.");
        }

        // Regla HU-13: garantizar autenticación/seguridad — la cuenta debe pertenecer al usuario
        if (account.getUser() == null || !account.getUser().getId().equals(user.getId())) {
            auditService.logAction(user.getEmail(), "WITHDRAWAL_FAILED", "FAILED", "Intento de retiro sobre cuenta ajena " + accountNumber, "system");
            return ResponseEntity.status(403).body("No tienes permiso para retirar de esta cuenta.");
        }

        // Regla HU-13: validar saldo suficiente
        if (account.getBalance().compareTo(amount) < 0) {
            auditService.logAction(user.getEmail(), "WITHDRAWAL_FAILED", "FAILED", "Intento de retiro de " + amount + " con saldo insuficiente (" + account.getBalance() + ") en cuenta " + accountNumber, "system");
            return ResponseEntity.badRequest().body("Saldo insuficiente para realizar el retiro.");
        }

        // Descontar el monto del saldo disponible
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        // Registrar el movimiento de retiro en el historial
        com.banco.digital.models.Transaction transaction = com.banco.digital.models.Transaction.builder()
                .fromAccountId(account.getId())
                .amount(amount)
                .type("WITHDRAWAL")
                .description("Retiro de efectivo")
                .build();
        transactionRepository.save(transaction);

        auditService.logAction(user.getEmail(), "WITHDRAWAL", "SUCCESS", "Retiro de " + amount + " en cuenta " + accountNumber, "system");

        // Notificar al titular de la cuenta (HU-12)
        notificationService.notifyMovement(user, com.banco.digital.services.NotificationService.WITHDRAWAL, amount);

        return ResponseEntity.ok("Retiro de " + amount + " realizado con éxito en cuenta " + accountNumber);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(Authentication authentication, @RequestBody Map<String, String> request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Regla HU-08: Solo usuarios verificados (KYC)
        if (!user.isVerified()) {
            auditService.logAction(user.getEmail(), "CREATE_ACCOUNT_FAILED", "FAILED", "Intento de crear cuenta sin verificación KYC", "system");
            return ResponseEntity.status(403).body("Debes completar la verificación de identidad (KYC) antes de crear cuentas adicionales.");
        }

        String type = request.getOrDefault("type", "SAVINGS");
        
        // Generar número de cuenta aleatorio de 16 dígitos
        String accountNumber = java.util.stream.IntStream.range(0, 16)
                .mapToObj(i -> String.valueOf((int) (Math.random() * 10)))
                .reduce("", String::concat);

        Account newAccount = Account.builder()
                .type(type)
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build();

        Account saved = accountRepository.save(newAccount);
        auditService.logAction(user.getEmail(), "CREATE_ACCOUNT_SUCCESS", "SUCCESS", "Nueva cuenta creada: " + accountNumber + " (" + type + ")", "system");
        
        return ResponseEntity.ok(saved);
    }
}
