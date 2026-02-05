package com.example.demo.repository;

import com.example.demo.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByUserId(Long userId);

    Optional<BankAccount> findByAccountNumber(String accountNumber);
}
