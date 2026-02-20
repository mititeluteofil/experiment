package com.example.demo.controller;

import com.example.demo.dto.AccountResponse;
import com.example.demo.dto.CreateAccountRequest;
import com.example.demo.dto.TransactionResponse;
import com.example.demo.exception.BadRequestException;
import com.example.demo.model.BankAccount;
import com.example.demo.model.User;
import com.example.demo.service.BankAccountService;
import com.example.demo.service.TransactionService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final TransactionService transactionService;
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@RequestBody CreateAccountRequest request, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        return AccountResponse.from(bankAccountService.create(user, request));
    }

    @GetMapping
    public List<AccountResponse> list(Principal principal) {
        User user = userService.findByEmail(principal.getName());
        return bankAccountService.getAccountsForUser(user.getId()).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return AccountResponse.from(bankAccountService.getById(id));
    }

    /**
     * Deliberately naive transaction history endpoint:
     * - Returns ALL matching transactions in one response (no pagination)
     * - Filters applied in Java after loading everything from DB
     * - No limit on result size — a busy account will produce multi-MB responses
     */
    @GetMapping("/{id}/transactions")
    public List<TransactionResponse> transactions(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            Principal principal) {

        User user = userService.findByEmail(principal.getName());
        BankAccount account = bankAccountService.getById(id);

        if (!account.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not own this account");
        }

        return transactionService.getHistory(id, from, to, direction, minAmount, maxAmount)
                .stream()
                .map(tx -> TransactionResponse.from(tx, id))
                .toList();
    }
}
