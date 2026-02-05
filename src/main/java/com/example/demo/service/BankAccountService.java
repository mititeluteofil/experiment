package com.example.demo.service;

import com.example.demo.dto.CreateAccountRequest;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.BankAccount;
import com.example.demo.model.User;
import com.example.demo.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    @Transactional
    public BankAccount create(User user, CreateAccountRequest request) {
        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setAccountNumber(generateAccountNumber());
        if (request.currency() != null && !request.currency().isBlank()) {
            account.setCurrency(request.currency().toUpperCase());
        }
        return bankAccountRepository.save(account);
    }

    public List<BankAccount> getAccountsForUser(Long userId) {
        return bankAccountRepository.findByUserId(userId);
    }

    public BankAccount getById(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private String generateAccountNumber() {
        long number = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        return String.valueOf(number);
    }
}
