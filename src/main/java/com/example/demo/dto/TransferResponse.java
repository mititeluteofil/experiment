package com.example.demo.dto;

import com.example.demo.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String currency,
        String status,
        String description,
        LocalDateTime createdAt
) {
    public static TransferResponse from(Transaction tx) {
        return new TransferResponse(
                tx.getId(),
                tx.getFromAccount().getAccountNumber(),
                tx.getToAccount().getAccountNumber(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus().name(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
