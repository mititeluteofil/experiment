package com.example.ledger;

public record Entry(Type type, long amount, long balanceAfter) {

    public enum Type {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER_OUT,
        TRANSFER_IN
    }
}
