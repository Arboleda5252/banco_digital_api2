package com.banco.digital.controllers;

import com.banco.digital.models.Account;
import com.banco.digital.models.Transaction;
import com.banco.digital.repositories.AccountRepository;
import com.banco.digital.repositories.TransactionRepository;
import com.banco.digital.repositories.UserRepository;
import com.banco.digital.services.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la regla de negocio de la HU-11 (Depositar dinero):
 * el sistema debe validar que el monto sea mayor a cero y actualizar el saldo.
 */
@ExtendWith(MockitoExtension.class)
class AccountControllerDepositTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AccountController controller;

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .type("SAVINGS")
                .accountNumber("1234567890123456")
                .balance(new BigDecimal("100.00"))
                .build();
    }

    private Map<String, Object> request(String accountNumber, Object amount) {
        Map<String, Object> req = new HashMap<>();
        req.put("accountNumber", accountNumber);
        req.put("amount", amount);
        return req;
    }

    @Test
    void depositoValido_aumentaSaldoYRegistraTransaccion() {
        when(accountRepository.findByAccountNumber("1234567890123456")).thenReturn(Optional.of(account));

        ResponseEntity<?> response = controller.deposit(request("1234567890123456", "50.00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void montoCero_esRechazado() {
        ResponseEntity<?> response = controller.deposit(request("1234567890123456", "0"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void montoNegativo_esRechazado() {
        ResponseEntity<?> response = controller.deposit(request("1234567890123456", "-25.00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void montoNoNumerico_esRechazado() {
        ResponseEntity<?> response = controller.deposit(request("1234567890123456", "abc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void cuentaInexistente_devuelve404() {
        when(accountRepository.findByAccountNumber("0000000000000000")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.deposit(request("0000000000000000", "50.00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(transactionRepository, never()).save(any());
    }
}
