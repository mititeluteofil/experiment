package com.example.demo.agent;

import com.example.demo.dto.*;
import com.example.demo.exception.BadRequestException;
import com.example.demo.model.BankAccount;
import com.example.demo.model.User;
import com.example.demo.service.*;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BankingTools {

    private final UserService        userService;
    private final BankAccountService bankAccountService;
    private final TransactionService transactionService;
    private final TransferService    transferService;

    @Value("${agent.max-transfer-amount:10000}")
    private BigDecimal maxTransferAmount;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private User currentUser() {
        return userService.findByEmail(currentUserEmail());
    }

    @Tool("Get the profile of the currently authenticated user (name and email)")
    public UserResponse getCurrentUser() {
        return UserResponse.from(currentUser());
    }

    @Tool("List all bank accounts belonging to the current user")
    public List<AccountResponse> listAccounts() {
        return bankAccountService.getAccountsForUser(currentUser().getId())
                .stream().map(AccountResponse::from).toList();
    }

    @Tool("Get details of a specific bank account by its numeric ID")
    public AccountResponse getAccount(Long accountId) {
        BankAccount account = bankAccountService.getById(accountId);
        if (!account.getUser().getEmail().equals(currentUserEmail()))
            throw new BadRequestException("Account does not belong to you");
        return AccountResponse.from(account);
    }

    @Tool("""
            Get transaction history for a bank account. All filters are optional.
            direction: IN, OUT, or ALL (default ALL).
            fromDate / toDate: ISO date-time strings like 2024-01-01T00:00:00, or null for no limit.
            minAmount / maxAmount: decimal amounts, or null for no limit.
            """)
    public List<TransactionResponse> getTransactionHistory(
            Long accountId,
            String direction,
            String fromDate,
            String toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        // ownership check
        getAccount(accountId);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime from = fromDate != null && !fromDate.isBlank() ? LocalDateTime.parse(fromDate, fmt) : null;
        LocalDateTime to   = toDate   != null && !toDate.isBlank()   ? LocalDateTime.parse(toDate,   fmt) : null;
        return transactionService.getHistory(accountId, from, to, direction, minAmount, maxAmount)
                .stream().map(tx -> TransactionResponse.from(tx, accountId)).toList();
    }

    @Tool("Create a new bank account for the current user. Provide a 3-letter ISO currency code such as USD or EUR.")
    public AccountResponse createAccount(String currency) {
        return AccountResponse.from(
                bankAccountService.create(currentUser(), new CreateAccountRequest(currency)));
    }

    @Tool("Transfer money between two accounts. Maximum $10,000 per transaction. Always confirm details with the user before calling this tool.")
    public TransferResponse transferFunds(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("Transfer amount must be positive");
        if (amount.compareTo(maxTransferAmount) > 0)
            throw new BadRequestException(
                    "Amount $" + amount + " exceeds the per-transaction limit of $" + maxTransferAmount);
        return TransferResponse.from(
                transferService.transfer(
                        new TransferRequest(fromAccountId, toAccountId, amount, description),
                        currentUserEmail()));
    }
}
