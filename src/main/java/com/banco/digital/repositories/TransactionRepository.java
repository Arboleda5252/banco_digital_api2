package com.banco.digital.repositories;

import com.banco.digital.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT t FROM Transaction t WHERE t.fromAccountId IN ?1 OR t.toAccountId IN ?1 ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountIds(List<Long> accountIds);
}
