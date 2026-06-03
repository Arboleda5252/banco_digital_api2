package com.banco.digital.repositories;

import com.banco.digital.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByDocumentNumber(String documentNumber);
    Optional<User> findByResetToken(String resetToken);
}
