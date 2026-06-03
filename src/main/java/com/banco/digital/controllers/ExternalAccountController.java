package com.banco.digital.controllers;

import com.banco.digital.models.ExternalAccount;
import com.banco.digital.models.User;
import com.banco.digital.repositories.ExternalAccountRepository;
import com.banco.digital.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/external-accounts")
public class ExternalAccountController {

    private final ExternalAccountRepository externalAccountRepository;
    private final UserRepository userRepository;

    public ExternalAccountController(ExternalAccountRepository externalAccountRepository, UserRepository userRepository) {
        this.externalAccountRepository = externalAccountRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ExternalAccount>> getMyExternalAccounts(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(externalAccountRepository.findByUserId(user.getId()));
    }

    @PostMapping
    public ResponseEntity<ExternalAccount> linkAccount(Authentication authentication, @RequestBody ExternalAccount request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        request.setUser(user);
        return ResponseEntity.ok(externalAccountRepository.save(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExternalAccount(Authentication authentication, @PathVariable Long id) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ExternalAccount acc = externalAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!acc.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized to delete this account");
        }
        
        externalAccountRepository.delete(acc);
        return ResponseEntity.ok().build();
    }
}
