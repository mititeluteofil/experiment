package com.example.ledger;

import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerConcurrencyTest {

    @RepeatedTest(5)
    void concurrentDepositsDoNotLoseUpdates() throws InterruptedException {
        int threads = 8;
        int depositsPerThread = 1_000;
        Ledger ledger = new Ledger();
        ledger.open("acc-1", 0);
        CountDownLatch startGate = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    startGate.await();
                    for (int i = 0; i < depositsPerThread; i++) {
                        ledger.deposit("acc-1", 1);
                    }
                    return null;
                });
            }
            startGate.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertEquals((long) threads * depositsPerThread, ledger.balanceOf("acc-1"));
    }

    @RepeatedTest(3)
    void crossingTransfersDoNotDeadlock() throws InterruptedException {
        int transfersPerThread = 5_000;
        Ledger ledger = new Ledger();
        ledger.open("acc-A", 1_000_000);
        ledger.open("acc-B", 1_000_000);
        CountDownLatch startGate = new CountDownLatch(1);

        // daemon threads: if the naive locking deadlocks, the test fails on the
        // timeout below and the JVM can still exit
        ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        try {
            executor.submit(() -> {
                startGate.await();
                for (int i = 0; i < transfersPerThread; i++) {
                    ledger.transfer("acc-A", "acc-B", 1);
                }
                return null;
            });
            executor.submit(() -> {
                startGate.await();
                for (int i = 0; i < transfersPerThread; i++) {
                    ledger.transfer("acc-B", "acc-A", 1);
                }
                return null;
            });
            startGate.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                    "crossing transfers did not finish -- deadlock");
        } finally {
            executor.shutdownNow();
        }

        assertEquals(2_000_000, ledger.balanceOf("acc-A") + ledger.balanceOf("acc-B"));
    }
}
