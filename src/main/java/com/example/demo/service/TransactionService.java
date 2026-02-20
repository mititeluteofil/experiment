package com.example.demo.service;

import com.example.demo.model.Transaction;
import com.example.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Deliberately naive transaction history:
     * 1. Loads ALL transactions for the account into memory (no pagination)
     * 2. Filters in Java instead of pushing predicates to SQL
     * 3. Returns the entire result set in one response
     *
     * This will blow up when an account has thousands of transactions:
     * - GC pressure from large List<Transaction>
     * - Slow response times from serializing huge payloads
     * - DB doing a full scan with no useful indexes
     */
    public List<Transaction> getHistory(Long accountId,
                                        LocalDateTime from,
                                        LocalDateTime to,
                                        String direction,
                                        BigDecimal minAmount,
                                        BigDecimal maxAmount) {

        // Load ALL transactions — no limit, no pagination
        List<Transaction> all = transactionRepository.findAllByAccountId(accountId);

        // Filter in Java instead of SQL — wastes memory and CPU
        return all.stream()
                .filter(tx -> from == null || !tx.getCreatedAt().isBefore(from))
                .filter(tx -> to == null || !tx.getCreatedAt().isAfter(to))
                .filter(tx -> {
                    if (direction == null || direction.equalsIgnoreCase("ALL")) return true;
                    if (direction.equalsIgnoreCase("IN")) return tx.getToAccount().getId().equals(accountId);
                    if (direction.equalsIgnoreCase("OUT")) return tx.getFromAccount().getId().equals(accountId);
                    return true;
                })
                .filter(tx -> minAmount == null || tx.getAmount().compareTo(minAmount) >= 0)
                .filter(tx -> maxAmount == null || tx.getAmount().compareTo(maxAmount) <= 0)
                .toList();
    }
}
