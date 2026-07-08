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
}
