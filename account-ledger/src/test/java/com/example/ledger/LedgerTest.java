package com.example.ledger;

import org.junit.jupiter.api.Test;

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
}
