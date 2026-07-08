package com.example.ledger;

/**
 * Interview skeleton — handed out as-is at the start. Money is a long in
 * minor units (cents); never a floating-point type.
 */
public class Account {

    private final String id;
    private long balance;

    public Account(String id, long initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }

    public String id() {
        return id;
    }

    long balance() {
        return balance;
    }

    void add(long amount) {
        balance += amount;
    }

    void subtract(long amount) {
        if (balance < amount) {
            throw new IllegalStateException("Insufficient funds on " + id);
        }
        balance -= amount;
    }
}
