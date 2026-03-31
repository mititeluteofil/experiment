package com.example.demo.dto;

import com.example.demo.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String currency,
        String status,
        String direction,
        String description,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction tx, Long viewingAccountId) {
        String direction = tx.getToAccount().getId().equals(viewingAccountId) ? "IN" : "OUT";
        return new TransactionResponse(
                tx.getId(),
                tx.getFromAccount().getAccountNumber(),
                tx.getToAccount().getAccountNumber(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus().name(),
                direction,
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
