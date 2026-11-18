# Revision weak list

Maintained by Claude. Anything scored below 2 lands here and gets re-asked
later the same day and at the start of every session until it scores 2 twice.

Score: 2 = clear and complete, 1 = partly, 0 = blank.
Answer shape: **What → Why (problem) → How in TradeFlow → Trade-off**.

| # | Topic / question | Added | Score history | Status |
|---|---|---|---|---|
| 1 | Which topic has the DLT, and which consumer's error handler sends there? | Tue 17 (drawing) | 1 | open |
| 2 | Idempotency (processed_events by eventId) vs status guard (`ensurePending`) — two different protections | Tue 17 (drawing) | 1, 1 (idempotency yes, status guard missing) | open |
| 3 | The key words for the outbox save: "**same transaction**" — and what problem that solves (dual-write) | Tue 17 (drawing) | 1, 1 (said "same txn", no dual-write) | open |
| 4 | DTO vs entity: 3 reasons — hide fields, **API contract decoupled from DB schema**, **avoid lazy-loading / Jackson recursion** | Tue 17 (M1 Q1) | 1 | open |
| 5 | N+1 in TradeFlow: **list page** of orders → items per order; fix = `hibernate.default_batch_fetch_size: 20` (`order_id = any(?)`), 4 → 3 queries; why not JOIN FETCH (paging in memory) | Tue 17 (M2 Q3) | 1 | open |
| 6 | `@Transactional(readOnly = true)`: no dirty checking / no snapshots / no flush + read-only hint to the connection (replica routing). NOT "allows parallel reads" | Tue 17 (M2 Q5) | 0 | open |
| 7 | Aggregate root = **consistency boundary**: all changes go through `Order` methods (`addItem`, `ensurePending`) so rules hold; one repo per aggregate; others refer by id | Tue 17 (M2 Q7) | 1 | open |
| 8 | Validation failure → `MethodArgumentNotValidException` → our `GlobalExceptionHandler` → **400 `ProblemDetail`** (RFC 9457, `application/problem+json`) with an `errors` map field → message | Tue 17 (M1 Q8) | 1 | open |
| 9 | Offset = position of a message **in a partition**; committed offset = a **group's** progress per partition; `auto-offset-reset` applies **only when the group has no committed offset** | Wed 18 (M3 Q1, Q6) | 1 | open |
| 10 | Fan-out: each service gets every message because they use **different group ids**; each group keeps its own offsets (same group = split) | Wed 18 (M3 Q5) | 1 | open |
| 11 | Own event class per service: no shared jar → **deploy independently**, contract = the JSON, not a Java class; trade-off = drift → schema registry | Wed 18 (M3 Q7) | 1 | open |
