package com.example.demo.controller;

import com.example.demo.dto.AccountResponse;
import com.example.demo.dto.CreateAccountRequest;
import com.example.demo.model.User;
import com.example.demo.service.BankAccountService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;
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
}
