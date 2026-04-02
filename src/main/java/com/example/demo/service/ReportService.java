package com.example.demo.service;

import com.example.demo.dto.ReportPeriod;
import com.example.demo.dto.ReportResponse;
import com.example.demo.exception.BadRequestException;
import com.example.demo.model.BankAccount;
import com.example.demo.model.Transaction;
import com.example.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final BankAccountService bankAccountService;

    /**
     * Deliberately naive report generation:
     * 1. Loads ALL transactions for the account from DB (no date filtering in SQL)
     * 2. Filters by date range in Java
     * 3. Aggregates in Java using streams (no SQL SUM/COUNT)
     * 4. No caching — identical report recomputed on every request
     * 5. Synchronous — blocks the request thread while aggregating
     *
     * This will become painfully slow for accounts with large transaction volumes
     * and will hammer the DB with redundant full-table scans.
     */
    public ReportResponse generate(Long accountId, ReportPeriod period,
                                   LocalDateTime customFrom, LocalDateTime customTo) {

        BankAccount account = bankAccountService.getById(accountId);

        LocalDateTime from;
        LocalDateTime to;

        switch (period) {
            case WEEKLY -> {
                to = LocalDateTime.now();
                from = to.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
            }
            case MONTHLY -> {
                to = LocalDateTime.now();
                from = to.withDayOfMonth(1).toLocalDate().atStartOfDay();
            }
            case CUSTOM -> {
                if (customFrom == null || customTo == null) {
                    throw new BadRequestException("Custom period requires both 'from' and 'to' parameters");
                }
                from = customFrom;
                to = customTo;
            }
            default -> throw new BadRequestException("Unknown period: " + period);
        }

        // Naive: load ALL transactions, then filter in Java
        List<Transaction> all = transactionRepository.findAllByAccountId(accountId);

        List<Transaction> filtered = all.stream()
                .filter(tx -> !tx.getCreatedAt().isBefore(from) && !tx.getCreatedAt().isAfter(to))
                .toList();

        // Aggregate in Java instead of SQL — O(n) scan every time
        BigDecimal totalSent = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal largestSent = BigDecimal.ZERO;
        BigDecimal largestReceived = BigDecimal.ZERO;
        long count = filtered.size();

        for (Transaction tx : filtered) {
            if (tx.getFromAccount().getId().equals(accountId)) {
                totalSent = totalSent.add(tx.getAmount());
                if (tx.getAmount().compareTo(largestSent) > 0) {
                    largestSent = tx.getAmount();
                }
            }
            if (tx.getToAccount().getId().equals(accountId)) {
                totalReceived = totalReceived.add(tx.getAmount());
                if (tx.getAmount().compareTo(largestReceived) > 0) {
                    largestReceived = tx.getAmount();
                }
            }
        }

        BigDecimal netChange = totalReceived.subtract(totalSent);

        return new ReportResponse(
                accountId,
                account.getAccountNumber(),
                period,
                from,
                to,
                totalSent,
                totalReceived,
                netChange,
                count,
                largestSent,
                largestReceived
        );
    }
}
