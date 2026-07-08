# Account Ledger — Revolut-style Interview Plan (TDD, commit by commit)

This module lives inside the `experiment` repo next to the phased banking web
app (see the root `PLAN.md`). The web app explores the same problems at REST/JPA
scale; this module is the interview-scale distillation: **a thread-safe
in-memory account ledger, built from a given skeleton in 60–90 minutes.**

Revolut hands you a skeleton (`Account`, `Ledger` with `UnsupportedOperationException`
stubs — exactly what this module's first commit contains) and reveals tasks one
at a time. Red → green cycles, commit only when green.

Run everything from the `experiment` directory:

```
gradlew.bat :account-ledger:test
gradlew.bat :account-ledger:test --tests "com.example.ledger.LedgerTest"
```

Branches: `main` is the answer key (this plan implemented commit by commit);
`rehearsal` starts at the skeleton with `REHEARSAL.md`, the self-build guide.

## The task sequence

| Task | What the interviewer asks | Time box |
|------|---------------------------|----------|
| 1 | Open accounts, read balances | ~10 min |
| 2 | Deposit / withdraw with an insufficient-funds guard | ~15 min |
| 3 | Transfer between accounts | ~10 min |
| 4 | Make it correct under concurrency: no lost updates, no deadlock, money conserved | ~25 min |
| 5 (stretch) | Per-account statement (audit trail) | if time |

## Design bets made up front

1. **Money is `long` minor units.** Never `double`/`float` — say it before the
   interviewer asks; it's the cheapest point in the session. (`BigDecimal` is the
   other defensible answer; `long` cents is simpler under concurrency.)
2. **Per-account monitors, not one global lock.** `synchronized (account)` keeps
   unrelated accounts fully parallel. A single lock on the ledger is "correct"
   and an instant scalability follow-up you'd rather preempt.
3. **`ConcurrentHashMap` for the registry, `putIfAbsent` for open.** Account
   creation is its own check-then-act; kill it at birth.
4. **Lock ordering by account id for two-account operations.** Deadlock is not
   fixed after it happens — it's made impossible by construction. This is THE
   signature move of the exercise; the plan makes it a dramatic red first.
5. **Invariant-based concurrency tests.** "Total money is constant" survives any
   thread interleaving; testing exact intermediate states does not.

## Commit-by-commit walkthrough

### Task 1 — open / balance

**Commit 1 — `feat: open account and read balance`**
- Red: `openedAccountHasInitialBalance`, `balanceOfUnknownAccountThrows`,
  `openingDuplicateAccountThrows` (stubs throw `UnsupportedOperationException` —
  a different-flavored red than the compile-error kind).
- Green: `ConcurrentHashMap<String, Account>`; `open` uses `putIfAbsent` and
  throws `IllegalArgumentException` on duplicates; a private `require(accountId)`
  helper for lookups.

### Task 2 — deposit / withdraw

**Commit 2 — `feat: deposit and withdraw with funds guard`**
- Red: `depositIncreasesBalance`, `withdrawDecreasesBalance`,
  `withdrawBeyondBalanceThrowsAndLeavesBalanceUntouched`,
  `nonPositiveAmountsAreRejected`.
- Green: plain `balance += / -=` mutations on `Account` (package-private
  `add`/`subtract` with the guard in `subtract`). **No synchronization yet —
  plant the flag out loud**: "`+=` is a read-modify-write; I'll make it safe in
  the concurrency task."

### Task 3 — transfer

**Commit 3 — `feat: transfer moves money between accounts`**
- Red: `transferMovesMoney`, `transferWithInsufficientFundsLeavesBothUntouched`,
  `transferToSameAccountIsRejected`.
- Green: compose withdraw-then-deposit (withdraw's guard runs first, so failure
  leaves both untouched). Reject self-transfer — it's a business nonsense case
  AND it removes an edge from the locking discussion later.

### Task 4 — concurrency (the real interview)

**Commit 4 — `test+fix: concurrent deposits do not lose updates`**
- Red: 8 latch-gated threads × 1,000 × `deposit(acc, 1)` → balance must be
  exactly 8,000. `+=` loses updates — probabilistic red, `@RepeatedTest`.
- Green: `synchronized (account)` around every balance access in `Ledger`
  (deposit, withdraw, balanceOf — the read needs the monitor too, for
  visibility). Transfer naively takes both monitors nested, from-then-to.
  **Say it: "nested locks in caller-supplied order — that's a deadlock waiting
  for crossing transfers."** You just planted commit 5's red.

**Commit 5 — `test+fix: crossing transfers must not deadlock`**
- Red: two threads, `A→B` × 5,000 and `B→A` × 5,000, on **daemon** threads
  (so the JVM can die even when they can't), `awaitTermination(5s)` must return
  true. Naive nested locking deadlocks almost instantly.
- Green: acquire monitors in a global order — smaller `id` first:
  ```java
  Account first = from.id().compareTo(to.id()) < 0 ? from : to;
  Account second = first == from ? to : from;
  synchronized (first) {
      synchronized (second) {
          from.subtract(amount);
          to.add(amount);
      }
  }
  ```
  Narrate: any consistent global order kills circular wait — one of the four
  Coffman conditions gone, deadlock impossible by construction.

**Commit 6 — `test: money is conserved under concurrent random transfers`**
- Should be green on first run — say so, then prove it: 4 accounts × 10,000
  start; 8 threads × 2,000 random transfers (random pairs, random amounts,
  insufficient-funds swallowed); total is exactly 40,000 after the storm.
  This is the invariant test — it would have caught lost updates, partial
  transfers, and torn reads all at once.

### Task 5 (stretch) — statement

**Commit 7 — `feat: per-account statement`**
- Red: `statementRecordsOperationsInOrder` — deposit, withdraw, transfer out/in
  produce typed entries with `balanceAfter`; statement is an immutable snapshot.
- Green: `record Entry(Type type, long amount, long balanceAfter)`; entries
  appended to a per-account `ArrayList` **under the account monitor** (same
  lock as the balance mutation — entry and balance move atomically);
  `statement()` returns `List.copyOf` under the monitor.

## Final file layout

```
account-ledger/src/main/java/com/example/ledger/
    Ledger.java      // open, deposit, withdraw, transfer, balanceOf, statement
    Account.java     // balance + guarded mutations + entries; the monitor object
    Entry.java       // record: type, amount, balanceAfter
account-ledger/src/test/java/com/example/ledger/
    LedgerTest.java
    LedgerConcurrencyTest.java
```

## Pitfalls to not trip on live

- `double` for money. Instant credibility hit.
- One big `synchronized` on the Ledger — correct, but the follow-up is "now make
  unrelated accounts parallel" and you're refactoring under pressure.
- Forgetting `balanceOf` needs the monitor too — unsynchronized reads of a
  `long` have no visibility guarantee (and `long` isn't even tear-proof in
  theory). "Reads are synchronized for visibility" is the sentence.
- Locking `from` then `to` in argument order. The interviewer WILL run crossing
  transfers in their head; get to id-ordering before they do.
- Non-daemon threads in the deadlock test — a red run then hangs the JVM
  instead of failing cleanly.
- Testing concurrency with exact interleavings instead of invariants
  (conservation of money) — invariants survive any schedule.
