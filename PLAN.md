# Banking Application — Incremental Build Plan

## Tech Stack (starting point)
- Java 17, Spring Boot 3.2.5, Spring Data JPA
- H2 in-memory (will migrate to PostgreSQL when persistence becomes a problem)
- Spring Security (basic auth initially)

---

## Phase 1 — Models & CRUD (the foundation)

**Goal:** Get User and BankAccount entities persisted and exposed via REST.

**Models:**
- `User` — id, email, password (hashed), fullName, createdAt
- `BankAccount` — id, accountNumber (generated), balance (BigDecimal), currency, user (FK), createdAt

**Work:**
1. Entity classes with JPA annotations
2. Repositories (JpaRepository)
3. Service layer with basic validation
4. REST controllers: register user, create account, get accounts
5. Simple Spring Security config (permit registration, authenticate everything else)
6. Basic error handling (@ControllerAdvice)

**Deliberately naive choices (scalability seeds):**
- Balance stored directly on the account row (no derived balance)
- No pagination on any list endpoint
- No database indexes beyond PK
- H2 in-memory — data gone on restart

---

## Phase 2 — Money Transfers

**Goal:** Transfer money between accounts (own and other users').

**Models:**
- `Transaction` — id, fromAccount (FK), toAccount (FK), amount, currency, status (PENDING/COMPLETED/FAILED), description, createdAt

**Work:**
1. Transaction entity + repository
2. TransferService: debit source, credit target, record transaction
3. `POST /api/transfers` endpoint (fromAccountId, toAccountId, amount)
4. Validation: sufficient balance, accounts exist, same currency, no self-transfer to same account

**Deliberately naive choices:**
- `@Transactional` with default isolation level — no explicit locking
- Balance updated directly via `account.setBalance(account.getBalance().subtract(amount))`
- No idempotency key — duplicate requests = duplicate transfers
- Synchronous processing only

**Scalability problems that will surface:**
- **Lost updates / race conditions** — two concurrent transfers from the same account can both read the same balance, both pass validation, both deduct → overdraft
- **No idempotency** — network retries cause double-spend

---

## Phase 3 — Transaction History

**Goal:** Users can view transaction history for any of their accounts.

**Work:**
1. `GET /api/accounts/{id}/transactions` — list all transactions
2. Filter by date range, direction (in/out), min/max amount
3. Return full list sorted by date descending

**Deliberately naive choices:**
- Load ALL transactions from DB, return them in one response
- No pagination, no cursor, no limit
- Filtering done via JPQL with dynamic query building (string concat)

**Scalability problems that will surface:**
- **Memory blowup** — an active account accumulates thousands of transactions; loading all into a List<Transaction> causes GC pressure and slow responses
- **Slow queries** — no indexes on createdAt or accountId FK columns (beyond JPA defaults)
- **Payload size** — 10k transactions serialized to JSON = multi-MB response

---

## Phase 4 — Reports

**Goal:** Generate spending/income reports for weekly, monthly, and custom date ranges.

**Model:**
- No new table yet — compute on the fly

**Work:**
1. `GET /api/accounts/{id}/reports?period=WEEKLY|MONTHLY|CUSTOM&from=...&to=...`
2. Aggregate: total sent, total received, net change, number of transactions, largest transaction
3. Return a ReportDTO

**Deliberately naive choices:**
- Compute the report synchronously on request by loading all matching transactions into memory and aggregating in Java
- No caching — same report computed fresh every time
- No pre-aggregation — raw transaction scan every time

**Scalability problems that will surface:**
- **Slow API responses** — aggregating a year of transactions blocks the request thread for seconds
- **DB load** — heavy `SUM()`/`COUNT()` queries on the transaction table compete with live transfer writes
- **Repeated work** — the same weekly report is re-computed on every request

---

## Phase 5 — Fix: Transfer Correctness (first scalability fix)

**Problems addressed:** Race conditions, lost updates, double-spend from Phase 2.

**Solutions:**
1. **Pessimistic locking** — `@Lock(PESSIMISTIC_WRITE)` on account reads during transfer (or `SELECT ... FOR UPDATE`)
2. **Idempotency key** — client sends a unique key per transfer; server deduplicates via a unique constraint
3. **Ordered locking** — always lock the lower account ID first to prevent deadlocks

---

## Phase 6 — Fix: Transaction History Performance

**Problems addressed:** Memory blowup, slow queries from Phase 3.

**Solutions:**
1. **Cursor-based pagination** — return pages of 50, keyed by (createdAt, id) for stable ordering
2. **Database indexes** — composite index on `(from_account_id, created_at)` and `(to_account_id, created_at)`
3. **Spring Data Pageable** — use `Pageable` + `Slice<Transaction>` instead of `List`
4. **Migrate to PostgreSQL** — H2 won't cut it for real indexing and query planning

---

## Phase 7 — Fix: Report Performance

**Problems addressed:** Slow, blocking, redundant report computation from Phase 4.

**Solutions (pick incrementally):**
1. **Push aggregation to SQL** — replace Java-side aggregation with `SUM()`, `COUNT()`, `MAX()` queries so the DB does the work
2. **Async generation** — `@Async` or a simple job queue; return 202 Accepted + poll for result
3. **Caching** — cache completed period reports (a finished week's report never changes); use Spring `@Cacheable`
4. **Materialized view / summary table** — nightly job that pre-aggregates daily totals into a `DailyAccountSummary` table; reports query the summary instead of raw transactions

---

## Future Phases (when the above is solid)

- **Connection pool tuning** (HikariCP) when DB connections become a bottleneck
- **Read replica** — route report queries to a replica to stop them from competing with writes
- **Event-driven architecture** — publish transfer events to a message broker; consumers build read models (CQRS lite)
- **Rate limiting** on transfer endpoint
- **Audit log** as a separate append-only table

---

## Implementation Order Summary

| Phase | What                        | Naive? | Fixes What?               |
|-------|-----------------------------|--------|---------------------------|
| 1     | User + BankAccount CRUD     | Yes    | —                         |
| 2     | Money transfers             | Yes    | —                         |
| 3     | Transaction history         | Yes    | —                         |
| 4     | Reports                     | Yes    | —                         |
| 5     | Locking + idempotency       | No     | Phase 2 race conditions   |
| 6     | Pagination + indexes        | No     | Phase 3 memory/perf       |
| 7     | Async reports + caching     | No     | Phase 4 blocking/perf     |
