package com.example.ledger;

/**
 * Interview skeleton — handed out as-is at the start. Money is a long in
 * minor units (cents); never a floating-point type.
 */
public class Account {

    private final String id;
    private long balance;
    private final java.util.List<Entry> entries = new java.util.ArrayList<>();

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

    // call only while holding this account's monitor: entry and balance must agree
    void record(Entry.Type type, long amount) {
        entries.add(new Entry(type, amount, balance));
    }

    java.util.List<Entry> entries() {
        return java.util.List.copyOf(entries);
    }
}
