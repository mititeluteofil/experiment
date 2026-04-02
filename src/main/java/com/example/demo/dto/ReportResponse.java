package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReportResponse(
        Long accountId,
        String accountNumber,
        ReportPeriod period,
        LocalDateTime from,
        LocalDateTime to,
        BigDecimal totalSent,
        BigDecimal totalReceived,
        BigDecimal netChange,
        long transactionCount,
        BigDecimal largestSent,
        BigDecimal largestReceived
) {}
