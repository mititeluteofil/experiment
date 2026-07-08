package com.example.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerTest {

    @Test
    void openedAccountHasInitialBalance() {
        Ledger ledger = new Ledger();

        ledger.open("acc-1", 1_000);

        assertEquals(1_000, ledger.balanceOf("acc-1"));
    }

    @Test
    void balanceOfUnknownAccountThrows() {
        Ledger ledger = new Ledger();

        assertThrows(IllegalArgumentException.class, () -> ledger.balanceOf("missing"));
    }

    @Test
    void openingDuplicateAccountThrows() {
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 1_000);

        assertThrows(IllegalArgumentException.class, () -> ledger.open("acc-1", 500));
    }

    @Test
    void depositIncreasesBalance() {
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 1_000);

        ledger.deposit("acc-1", 250);

        assertEquals(1_250, ledger.balanceOf("acc-1"));
    }

    @Test
    void withdrawDecreasesBalance() {
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 1_000);

        ledger.withdraw("acc-1", 400);

        assertEquals(600, ledger.balanceOf("acc-1"));
    }

    @Test
    void withdrawBeyondBalanceThrowsAndLeavesBalanceUntouched() {
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 100);

        assertThrows(IllegalStateException.class, () -> ledger.withdraw("acc-1", 101));
        assertEquals(100, ledger.balanceOf("acc-1"));
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 1_000);

        assertThrows(IllegalArgumentException.class, () -> ledger.deposit("acc-1", 0));
        assertThrows(IllegalArgumentException.class, () -> ledger.withdraw("acc-1", -5));
        assertEquals(1_000, ledger.balanceOf("acc-1"));
    }

    @Test
    void transferMovesMoney() {
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 1_000);
        ledger.open("acc-2", 200);

        ledger.transfer("acc-1", "acc-2", 300);

        assertEquals(700, ledger.balanceOf("acc-1"));
        assertEquals(500, ledger.balanceOf("acc-2"));
    }

    @Test
    void transferWithInsufficientFundsLeavesBothUntouched() {
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 100);
        ledger.open("acc-2", 200);

        assertThrows(IllegalStateException.class, () -> ledger.transfer("acc-1", "acc-2", 101));
        assertEquals(100, ledger.balanceOf("acc-1"));
        assertEquals(200, ledger.balanceOf("acc-2"));
    }

    @Test
    void transferToSameAccountIsRejected() {
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 1_000);

        assertThrows(IllegalArgumentException.class, () -> ledger.transfer("acc-1", "acc-1", 100));
    }

    @Test
    void statementRecordsOperationsInOrder() {
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 1_000);
        ledger.open("acc-2", 0);

        ledger.deposit("acc-1", 500);
        ledger.withdraw("acc-1", 200);
        ledger.transfer("acc-1", "acc-2", 300);

        assertEquals(List.of(
                new Entry(Entry.Type.DEPOSIT, 500, 1_500),
                new Entry(Entry.Type.WITHDRAWAL, 200, 1_300),
                new Entry(Entry.Type.TRANSFER_OUT, 300, 1_000)
        ), ledger.statement("acc-1"));
        assertEquals(List.of(
                new Entry(Entry.Type.TRANSFER_IN, 300, 300)
        ), ledger.statement("acc-2"));
    }

    @Test
    void statementOfUnknownAccountThrows() {
        Ledger ledger = new Ledger();

        assertThrows(IllegalArgumentException.class, () -> ledger.statement("missing"));
    }
}
