package com.banco.digital.repositories;

import com.banco.digital.models.ExternalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalAccountRepository extends JpaRepository<ExternalAccount, Long> {
    List<ExternalAccount> findByUserId(Long userId);
}
