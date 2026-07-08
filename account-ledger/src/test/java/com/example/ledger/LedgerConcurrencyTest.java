package com.example.ledger;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
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

    @Test
    void moneyIsConservedUnderConcurrentRandomTransfers() throws InterruptedException {
        List<String> accountIds = List.of("acc-0", "acc-1", "acc-2", "acc-3");
        Ledger ledger = new Ledger();
        accountIds.forEach(id -> ledger.open(id, 10_000));
        CountDownLatch startGate = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            for (int t = 0; t < 8; t++) {
                executor.submit(() -> {
                    startGate.await();
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    for (int i = 0; i < 2_000; i++) {
                        String from = accountIds.get(random.nextInt(accountIds.size()));
                        String to = accountIds.get(random.nextInt(accountIds.size()));
                        if (from.equals(to)) {
                            continue;
                        }
                        try {
                            ledger.transfer(from, to, random.nextLong(1, 100));
                        } catch (IllegalStateException insufficientFunds) {
                            // fine -- conservation is what we assert
                        }
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

        long total = accountIds.stream().mapToLong(ledger::balanceOf).sum();
        assertEquals(40_000, total);
    }
}
