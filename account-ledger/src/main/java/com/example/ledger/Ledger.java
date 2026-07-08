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
        Account account = require(accountId);
        synchronized (account) {  // reads take the monitor too, for visibility
            return account.balance();
        }
    }

    public void deposit(String accountId, long amount) {
        requirePositive(amount);
        Account account = require(accountId);
        synchronized (account) {
            account.add(amount);
        }
    }

    public void withdraw(String accountId, long amount) {
        requirePositive(amount);
        Account account = require(accountId);
        synchronized (account) {
            account.subtract(amount);
        }
    }

    public void transfer(String fromId, String toId, long amount) {
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account: " + fromId);
        }
        requirePositive(amount);
        Account from = require(fromId);
        Account to = require(toId);
        // global acquisition order (by id) removes circular wait: deadlock impossible by construction
        Account first = from.id().compareTo(to.id()) < 0 ? from : to;
        Account second = first == from ? to : from;
        synchronized (first) {
            synchronized (second) {
                from.subtract(amount);
                to.add(amount);
            }
        }
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }

    private Account require(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Unknown account: " + accountId);
        }
        return account;
    }
}
