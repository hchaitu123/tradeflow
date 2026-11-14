# TradeFlow — Claude's Running Log

Maintained by Claude. My own handwritten notes live in `NOTES.md`.
Session 1 detail: see `session-01-setup.md`.

---

## Current state (as of Sun 15 Nov 2026)

- `m1`, `m2` tagged on `main`. Branch `m3-kafka`: M3 complete (last feature commit `401bce0`).
- **M3 left:** user's NOTES.md M3 section → merge → `git tag m3` → M3 Q-bank. **Next: M4.**
- Flow: `POST /orders` → saved PENDING_RISK → `orders.created` → risk-service rules → `orders.risk-decided` → order-service `approve()`/`reject()`.
- Docker: Postgres 17 on **5433**, Kafka (apache/kafka:4.1.0, KRaft) on **9092**. Topics: `orders.created`, `orders.risk-decided` (3 partitions each).
- Tests: order-service 11, risk-service 4 — all green. Commands cheat sheet: `docker-kafka-commands.md`.

### Code map
```
order-service  com.chaitanya.tradeflow.order
├── domain/   Order (@Version, approve/reject, addItem), OrderItem (notional()), OrderSide, OrderStatus
├── repo/     OrderRepository
├── service/  OrderService (findById, findByAccountId, createOrder→publish, summary, applyRiskDecision)
├── event/    OrderCreatedEvent(+Item) [published], RiskDecidedEvent [consumed]
├── listener/ RiskDecidedListener   @KafkaListener("orders.risk-decided")
└── web/      OrderController, GlobalExceptionHandler, dto/

risk-service   com.chaitanya.tradeflow.risk   (no web, no DB)
├── event/    OrderCreatedEvent(+Item, side = String) [consumed], RiskDecidedEvent [published]
├── rules/    RiskRules (symbol whitelist, 1..10,000 qty, order total ≤ ₹10,00,000), RiskDecision
└── listener/ OrderCreatedListener   @KafkaListener("orders.created") → evaluate → send
```

---

## Session log

### S1 — Mon 2 Nov — Setup
Tools verified, Maven parent + order-service skeleton, wrapper moved to root,
fixed empty `<relativePath/>`, first push. Details in `session-01-setup.md`.

### S2 — Tue 3 Nov — Big picture + entity + repo + DTOs
- Clarified what TradeFlow is (see "System design" below).
- Component scanning: only the main class's package **and below**.
- Built `OrderSide` (BUY, SELL), `OrderStatus` (PENDING_RISK, APPROVED, REJECTED).
- Built `Order` entity — fixed along the way:
  `long` → `Long` id, `Date` → `Instant`, `@Entity(name)` → `@Table(name="orders")`,
  added `@GeneratedValue(IDENTITY)`, bare `@Enumerated` → `EnumType.STRING`,
  added `protected Order() {}`.
- `application.yaml`: fixed H2 URL, `show-sql`, `format_sql`, `open-in-view: false`, H2 console.
- Read the generated `create table orders` SQL (snake_case, enum text columns, numeric(38,2), all nullable, create-drop).
- `OrderRepository`, `CreateOrderRequest` (5 fields), `OrderResponse` (9 fields incl. status + rejectionReason).
- Learned the **5-question method** for designing any feature (below).

### S3 — Wed 4 Nov — Entity constructor + service
- `Order` constructor takes the 5 client values; sets `status = PENDING_RISK` and `createdAt = Instant.now()` itself. Getters only, no setters.
- `OrderService`: `@Service`, constructor injection, `@Transactional` on create,
  `@Transactional(readOnly = true)` on read, `map(OrderResponse::create).orElseThrow(...)`.
- `OrderResponse.create(Order)` static factory for entity → DTO mapping.
- `OrderNotFoundException extends RuntimeException`, builds its own message from the id.
- Bug: imported `jakarta.transaction.Transactional` → no `readOnly`. Fixed to Spring's.

### S4 — Fri 6 Nov — Controller + Postman + unit tests
- `OrderController`: `@RestController`, `@RequestMapping("/orders")`,
  `@GetMapping("/{id}")` + `@PathVariable`, `@PostMapping` + `@RequestBody`, 201 on create.
- Bugs fixed: `"/id"` vs `"/{id}"`, GET returning 201, method name collision, stray `org.apache.coyote.Response` import.
- Postman: POST/GET work. Break-it results:
  | Test | Got | Should be |
  |---|---|---|
  | GET missing id | 500 → fixed to 404 via `@ResponseStatus` | 404 |
  | `"side": "HOLD"` | 400 (enum protects) | 400 ✅ |
  | missing symbol | 201, saved `symbol: null` | 400 |
  | quantity -5 | 201, saved | 400 |
- Lesson: change **one thing per test** (HOLD masked the missing-symbol bug).
- Noticed: H2 + create-drop → ids restart at 1 after restart.
- `OrderServiceTest` (Mockito, no Spring): not-found throws, create → PENDING_RISK, findById found → right symbol.
- Created `NOTES.md` template for handwritten notes.

### S5 — Sat 7 / Sun 8 Nov — Controller test + M1 close
- `OrderServiceTest` + findById-found test; `OrderControllerTest` (`@WebMvcTest`): 404 on missing, 201 + body on create.
- 6 tests green. Merged `m1-order-service` → `main`, tagged `m1`. **M1 DONE.**
- M1 Q-bank: solid on Maven, BigDecimal, EnumType, status codes.
  **Revise:** Q5 no-args constructor, Q7 constructor injection, Q8 `readOnly`, Q10 unit vs `@WebMvcTest`, bonus (validation belongs on the request DTO + `@Valid`).

### M2-S1 — Mon 9 Nov — Postgres + dev profile
- `docker-compose.yml` (postgres:17, named volume `pgdata`), `application-dev.yaml` (`ddl-auto: update`), postgresql driver (runtime).
- Hit: YAML `volumes:` indented under `services`; **Homebrew postgres@14 already on 5432** → "Unable to determine Dialect" (really a failed login) → mapped Docker to **5433**; old IntelliJ run holding 8080.
- Proved tests use H2 (default profile) and dev uses Postgres 17 via log lines. Data survives restart. Committed `935bfd3`.
- Noted: `update` never drops columns → old symbol/price/... columns linger in `orders` (why prod = `validate` + Flyway).

### M2-S2 — Tue 10 Nov — Order 1 → many OrderItem
- `OrderItem` (`@ManyToOne(LAZY)`, `@JoinColumn("order_id")`, `@Column(nullable=false)`, package-private `setOrder`).
- `Order`: `@OneToMany(mappedBy="order", cascade=ALL, orphanRemoval=true)`, `addItem` sets both sides, `getItems()` unmodifiable. No `OrderItemRepository` (aggregate root).
- DTOs: `OrderItemRequest`, `OrderItemResponse.create(item)`, `OrderResponse` items via `stream().map(...).toList()`.
- Break-it: removed cascade → POST 201 with `orderItemId: null`, GET → `items: []`, **no error**, unit tests still green (mocked repo). Lesson: cascade; GET-after-POST; limits of mock tests.
- Concepts covered: aggregate root, cascade vs mapping, owning side (FK side), orphanRemoval vs cascade REMOVE, LAZY default for *ToOne = EAGER, LazyInitializationException + open-in-view, `.toList()` unmodifiable.

### M2-S3 — Wed 11 Nov — Validation + ProblemDetail
- User couldn't recall the design answers without hands-on → switched to "do first, recall after" with more scaffolding. Works better for new topics.
- `spring-boot-starter-validation`; `@NotNull/@NotEmpty @Valid` on `CreateOrderRequest`, `@NotBlank/@NotNull @Positive` on `OrderItemRequest`; `@Valid @RequestBody` in controller.
- `web/GlobalExceptionHandler` (`@RestControllerAdvice`): `OrderNotFoundException` → 404 ProblemDetail; `MethodArgumentNotValidException` → 400 + `errors` map via `toMap(getField, getDefaultMessage, merge)`. Removed `@ResponseStatus` (no HTTP in service layer).
- Postman: missing symbol / qty −5 / price 0 → 400 with all field errors at once; `items: []` → `items: must not be empty`.
- New test `createOrder_withNegativeQuantity_returns400` + `verifyNoInteractions`. 7 tests green. Commit `d20377d`.
- M1 backlog items for validation and error bodies are now fixed.

### M2-S4 — Wed 11 / Thu 12 Nov — Paging by account + N+1 + param errors
- `GET /orders?accountId=` → `Page<Order> findByAccountId(Long, Pageable)` → `page.map(OrderResponse::create)` → `new PagedModel<>(...)` (stable JSON).
- Hit: POSTs landed in H2 (IntelliJ "Current File" had no profile) → made "OrderService(dev)" run config with `SPRING_PROFILES_ACTIVE=dev`.
- Saw **N+1**: 4 queries for a page of 2 orders → `hibernate.default_batch_fetch_size: 20` → 3 queries (`where order_id = any(?)`). JOIN FETCH + paging = in-memory paging, so avoided.
- Missing `accountId` gave Spring's default error body → handlers for `MissingServletRequestParameterException` and `MethodArgumentTypeMismatchException` (`getName()`/`getValue()`). Commit `741e30c`.

### M2-S5 — Thu 12 Nov — Optimistic locking
- Reset DB (`docker compose down -v`) so `version` column starts clean.
- `@Version Long version`; `approve()` / `reject(reason)` instead of setters.
- `OrderOptimisticLockTest` (`@SpringBootTest`, **not** `@Transactional` — first-level cache would hand back the same object): two copies, save A, save B → `ObjectOptimisticLockingFailureException`.
- Handler → 409. Commit `20dceee`.

### M2-S6 — Fri 13 Nov — `GET /orders/summary` (Streams)
- `OrderSummaryResponse` (Long counts — `counting()` returns Long; enum/Boolean keys).
- `OrderItem.notional()`; service: `flatMap` items, `groupingBy(+counting)`, `groupingBy(+reducing(ZERO, notional, add))`, `partitioningBy(compareTo >= 0, counting)`. User's idea: `groupingBy` for side.
- Bugs: `counting()` outside `groupingBy` (closed too early), `>0` outside the lambda, missing `counting()`, extra `)`. Tip: one argument per line.
- Postman: empty account → `groupingBy` maps `{}`, `partitioningBy` → `{false:0, true:0}`. 200 not 404 (query result, not resource).
- Mockito test (Claude filled 3 blanks on request); BigDecimal assert via `compareTo` (scale). 9 green. Commit `9034a8b`.
- Caveat for interviews: in prod with big data, aggregate in SQL (`GROUP BY`), not in memory.

---

## System design (what we're building)

Mini broker order pipeline: client places order → order-service saves (PENDING_RISK) →
Kafka → risk-service checks rules → approved/rejected event → order-service updates →
notification-service logs it. Stops before any exchange.

| Service | Job | Owns DB? | Milestone |
|---|---|---|---|
| order-service | REST API, save, publish events, apply risk result | yes | M1 |
| risk-service | listen, apply rules, publish decision | no | M3 |
| notification-service | listen, "notify" (log) | no | M5 |

Example risk rules (M3): qty × price ≤ ₹10,00,000; symbol in allowed list; 1 ≤ qty ≤ max.
M2 change ahead: Order becomes a basket → `Order 1 → many OrderItem` (symbol/side/qty/price move to OrderItem).
Not building: UI, market data, matching, payments, auth (until M8), market orders.

---

### M3 — Sat 14 / Sun 15 Nov — Kafka: order ↔ risk loop
- Plan: weekend + 4 leave days; user kept step-by-step style; each step now opens with a one-line **Why**.
- **Kafka by hand first:** compose service (KRaft, no ZooKeeper), topic with 3 partitions, console producer/consumer. Same key → same partition (123 and 456 also collided — different keys *may* share one).
- **Producer (order-service):** `spring-boot-starter-kafka` (Boot 4 starter; plain `spring-kafka` skips auto-config). Key `StringSerializer` (String keys everywhere, readable in CLI), value `JacksonJsonSerializer` (Jackson 3). `OrderCreatedEvent.from(saved)` — event ≠ DTO: past-tense fact carrying what consumers need. `send` after `save` (id needed). Unit test: `@Mock KafkaTemplate` + `verify(...).send(eq(topic), any(), any(OrderCreatedEvent.class))`; `eq` from `ArgumentMatchers` (IntelliJ picked `AdditionalMatchers`).
- **Break-it:** Kafka stopped → `send` blocks ~60 s (max.block.ms) → exception → rollback → no row, id 3 gap (sequences never roll back). Led to the **dual-write problem** → outbox in M5.
- **risk-service module:** no web/DB. App exited immediately with no listener (nothing keeps JVM alive). Own copy of the event (`side` as String — tolerant reader). Consumer: `ErrorHandlingDeserializer` → `JacksonJsonDeserializer`, `use.type.headers: false` + `value.default.type` (answers "`__TypeId__` points to a class risk-service doesn't have"). Hit `NoClassDefFoundError tools/jackson/...` → needed `spring-boot-starter-jackson` (webmvc brought it in order-service).
- Saw committed offsets: second run read nothing old — `auto-offset-reset` only applies when the group has no offset.
- **Rules** as a pure class (no mocks needed). Bug: total check inside the item loop → moved out. Tests: boundary `MAX_QUANTITY + 1`, sum-only breach (5L + 6L), assert the **reason**.
- **Decision back:** `RiskDecidedEvent` (new eventId; public contract ≠ internal `RiskDecision`) → `orders.risk-decided` (created explicitly with 3 partitions; broker auto-create would give 1).
- **order-service consumer:** yaml first pointed at `risk.event.RiskDecidedEvent` (wrong app) → fixed. `applyRiskDecision` `@Transactional`, no `save()` — log showed `update … where order_id=? and version=?` from **dirty checking**; version 0 → 1.
- Orders 2/4/5 stay PENDING_RISK (consumed before decisions existed) → replay via offset reset = optional exercise.
- Live: POST → `PENDING_RISK`, GET right after → `APPROVED` = **eventual consistency**.
- Also: `@Controller` used on a listener → `@Component`; package folder `rules/RiskDecision/` → `rules/`.

### Roadmap additions (agreed Sat 14 Nov) — to close microservices interview gaps
- **M5:** Micrometer Tracing — one trace id across order → risk → order (Kafka headers), visible in logs.
- **M8:** small **account-service** (`GET /accounts/{id}/limit`); risk-service calls it over REST, wrapped in a Resilience4j **circuit breaker** (gives M8 a real sync call to protect).
- Theory only (final mock interview): API gateway, JWT/OAuth2, service discovery vs K8s DNS, saga compensation, CQRS, schema registry, contract tests.

---

## Decisions log

| Decision | Why |
|---|---|
| Spring Boot 4.1.2, Java 21 | current versions; old tutorials may not compile |
| Monorepo, Maven parent `packaging=pom` | one build, shared versions |
| Status `PENDING_RISK` (not NEW) | says what's happening |
| `accountId` = `Long` | |
| Wrapper types (`Long`, `Integer`) in entity + DTOs | null = missing, not silent 0 |
| `BigDecimal` for price, `Instant` for time | exact money; UTC point in time |
| `EnumType.STRING` | ORDINAL breaks if enum reordered |
| Entity sets its own initial state, no setters | always valid; M4 adds `approve()`/`reject(reason)` |
| DTOs as records; never return entity | API contract decoupled from DB |
| Mapping via `OrderResponse.create(order)` | one place, reused by both service methods |
| `RuntimeException` for not-found | no try/catch noise; rolls back `@Transactional` |
| IntelliJ free tier; start.spring.io website | Initializr wizard is paid |

---

## Gotchas hit (quick reference)

- Empty `<relativePath/>` → Maven skips disk for parent.
- `@Entity(name)` = JPQL name, `@Table(name)` = table name. `order` is reserved SQL.
- Bare `@Enumerated` = ORDINAL.
- `jakarta.transaction.Transactional` ≠ Spring's (no `readOnly`).
- `@RequestParam` (`?id=5`) vs `@PathVariable` (`/5`); braces matter in `"/{id}"`.
- Record accessors are `request.symbol()`, not `getSymbol()`.
- `new BigDecimal("1520.50")` not `new BigDecimal(1520.50)`.
- `@Mock` = dependency (fake), `@InjectMocks` = class under test (real).
- Boot 4: `@MockBean` removed → `@MockitoBean`; `@WebMvcTest` lives in `org.springframework.boot.webmvc.test.autoconfigure`.
- Boot 4 Kafka: use `spring-boot-starter-kafka`; JSON needs Jackson on the classpath (`spring-boot-starter-jackson` if no web starter). `JacksonJsonSerializer/Deserializer` replace deprecated `JsonSerializer`.
- `./mvnw -q dependency:tree` prints nothing (quiet hides INFO) — drop `-q`.
- Read stack traces from the **last `Caused by`**; `ClassNotFoundException`/`NoClassDefFoundError` = missing dependency.
- IntelliJ "Error unmarshaling return header / EOFException" on Maven reload = importer process crashed; reload again / invalidate caches.

---

## M2 backlog (found by breaking things)

- [x] Missing symbol saved as null → `@NotBlank` + `@Valid`; `@Column(nullable = false)`
- [x] Negative quantity accepted → `@Positive`
- [x] Error bodies have no message → `@ControllerAdvice` + `ProblemDetail` (replace `@ResponseStatus` on exception)
- [x] Data lost on restart → Postgres in docker-compose; `ddl-auto` not create-drop
- [ ] Price accepted as string `"90.90"` (lenient Jackson) — decide if strict
- [ ] Optional polish: `ResponseEntity.ok(...)` / `HttpStatus.CREATED` instead of `HttpStatusCode.valueOf(...)`
- [ ] M4: `approve()`/`reject()` only allowed from PENDING_RISK; Testcontainers instead of H2
- [ ] Later: `ddl-auto: validate` + Flyway; OrderService cleanup (unused `Collectors` import, constant at top)

---

## Interview Qs covered so far

- Multi-module Maven? Parent vs child POM? Why Maven wrapper? How is a parent POM resolved? What does `mvn verify` run?
- Component scanning scope and the silent-404 trap.
- Why enums over Strings? ORDINAL vs STRING?
- `@Entity(name)` vs `@Table(name)`? Why wrapper type for `@Id`? Why no-args constructor?
- Why BigDecimal? Why Instant (not Date / LocalDateTime)?
- What is open-in-view and why disable it? `ddl-auto` options for prod?
- How does a repository interface with no implementation work? (proxy)
- Why DTOs instead of returning the entity?
- Constructor vs field injection? Checked vs unchecked exceptions? Why not return null?
- `@Transactional(readOnly = true)` — what does it do?
- `@RequestParam` vs `@PathVariable`? 200 vs 201 vs 404 vs 500?
- Why a lambda in `assertThrows`? `thenReturn` vs `thenAnswer`? Limits of mock-based tests?
- Which microservices patterns this project covers (and which it skips, and why).
- M3: topic/partition/offset/consumer group; key → partition ordering; `auto-offset-reset`; at-least-once + idempotency; event vs DTO; why consumers own their copy of the event (`__TypeId__`); dual-write problem; eventual consistency; WAR-in-Tomcat vs embedded server; mock vs pure logic; `verify` for side effects; varargs.

---

## How we work

- I (Chaitanya) write the code; Claude gives hints first, reviews on "check now".
- Fading help: M1 I say what's next; M2 I write the class list first; M3+ I design alone.
- Rules: type don't paste, one goal per session, commit every session, break it on purpose.
