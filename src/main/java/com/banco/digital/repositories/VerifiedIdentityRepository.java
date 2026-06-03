package com.banco.digital.repositories;

import com.banco.digital.models.VerifiedIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerifiedIdentityRepository extends JpaRepository<VerifiedIdentity, Long> {
    Optional<VerifiedIdentity> findByDocumentNumber(String documentNumber);
}
