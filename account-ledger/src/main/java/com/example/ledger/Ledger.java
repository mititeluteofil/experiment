package com.example.ledger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interview skeleton — implement test-first, one task at a time.
 * All operations must end up safe under concurrent use.
 */
public class Ledger {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public void open(String accountId, long initialBalance) {
        if (accounts.putIfAbsent(accountId, new Account(accountId, initialBalance)) != null) {
            throw new IllegalArgumentException("Account already exists: " + accountId);
        }
    }

    public long balanceOf(String accountId) {
        return require(accountId).balance();
    }

    public void deposit(String accountId, long amount) {
        throw new UnsupportedOperationException("TODO");
    }

    public void withdraw(String accountId, long amount) {
        throw new UnsupportedOperationException("TODO");
    }

    public void transfer(String fromId, String toId, long amount) {
        throw new UnsupportedOperationException("TODO");
    }

    private Account require(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Unknown account: " + accountId);
        }
        return account;
    }
}
