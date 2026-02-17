package com.example.demo.repository;

import com.example.demo.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
            Long fromAccountId, Long toAccountId);

    /**
     * Deliberately naive: loads ALL transactions for an account in one shot.
     * No pagination, no limit. Will cause memory pressure as transaction volume grows.
     */
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findAllByAccountId(@Param("accountId") Long accountId);
}
