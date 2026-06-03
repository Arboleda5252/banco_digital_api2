package com.banco.digital.services;

import com.banco.digital.models.Account;
import com.banco.digital.models.Transaction;
import com.banco.digital.repositories.AccountRepository;
import com.banco.digital.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository, AuthService authService, AuditService auditService, NotificationService notificationService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void transfer(String fromNum, String toNum, java.math.BigDecimal amount, String description, String mfaCode) {
        Account fromAccount = accountRepository.findByAccountNumber(fromNum)
                .orElseThrow(() -> new RuntimeException("Cuenta de origen no encontrada"));
        
        // Validar MFA (HU-09)
        if (!authService.verifyMfaCode(fromAccount.getUser().getEmail(), mfaCode)) {
            auditService.logAction(fromAccount.getUser().getEmail(), "TRANSFER_FAILED", "FAILED", "MFA inválido para transferencia", "system");
            throw new RuntimeException("Código de seguridad inválido o expirado");
        }

        Account toAccount = accountRepository.findByAccountNumber(toNum)
                .orElseThrow(() -> new RuntimeException("Cuenta de destino no encontrada"));

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            auditService.logAction(fromAccount.getUser().getEmail(), "TRANSFER_FAILED", "FAILED", "Saldo insuficiente: " + amount, "system");
            throw new RuntimeException("Saldo insuficiente");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = Transaction.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(amount)
                .type("TRANSFER")
                .description(description)
                .build();
        
        transactionRepository.save(transaction);
        
        auditService.logAction(fromAccount.getUser().getEmail(), "TRANSFER_SUCCESS", "SUCCESS",
            String.format("Transferencia de %s a cuenta %s", amount, toNum), "system");

        // Notificar a ambas partes según sus preferencias (HU-12)
        notificationService.notifyMovement(fromAccount.getUser(), NotificationService.TRANSFER_SENT, amount);
        notificationService.notifyMovement(toAccount.getUser(), NotificationService.TRANSFER_RECEIVED, amount);
    }

    public List<com.banco.digital.dto.TransactionDTO> getHistory(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        List<Long> ids = accounts.stream().map(Account::getId).collect(Collectors.toList());
        if (ids.isEmpty()) return java.util.Collections.emptyList();
        
        List<Transaction> transactions = transactionRepository.findByAccountIds(ids);
        
        return transactions.stream().map(t -> {
            String fromMasked = "N/A";
            if (t.getFromAccountId() != null) {
                fromMasked = accountRepository.findById(t.getFromAccountId())
                        .map(acc -> maskAccountNumber(acc.getAccountNumber()))
                        .orElse("Unknown");
            }
            
            String toMasked = "N/A";
            if (t.getToAccountId() != null) {
                toMasked = accountRepository.findById(t.getToAccountId())
                        .map(acc -> maskAccountNumber(acc.getAccountNumber()))
                        .orElse("Unknown");
            }

            return com.banco.digital.dto.TransactionDTO.builder()
                    .id(t.getId())
                    .fromAccountId(t.getFromAccountId())
                    .toAccountId(t.getToAccountId())
                    .fromAccountMasked(fromMasked)
                    .toAccountMasked(toMasked)
                    .amount(t.getAmount())
                    .type(t.getType())
                    .description(t.getDescription())
                    .createdAt(t.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
