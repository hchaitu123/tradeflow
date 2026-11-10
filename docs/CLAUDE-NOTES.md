# TradeFlow — Claude's Running Log

Maintained by Claude. My own handwritten notes live in `NOTES.md`.
Session 1 detail: see `session-01-setup.md`.

---

## Current state (as of Fri 6 Nov 2026)

- Branch: `m1-order-service` (pushed). `main` = skeleton only. No tags yet.
- Working API on H2: `POST /orders` → 201, `GET /orders/{id}` → 200 / 404.
- Tests: 4 green (`contextLoads` + 3 Mockito tests in `OrderServiceTest`).
- **M1 left:** `@WebMvcTest` for controller → `./mvnw verify` → merge to `main` → `git tag m1` → M1 Q-bank.

### Code map (order-service)
```
com.chaitanya.tradeflow.order
├── OrderServiceApplication       @SpringBootApplication (scan root)
├── domain/  Order, OrderSide, OrderStatus
├── repo/    OrderRepository       extends JpaRepository<Order, Long>
├── service/ OrderService, OrderNotFoundException (@ResponseStatus 404)
└── web/     OrderController
    └── dto/ CreateOrderRequest, OrderResponse (records)
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

---

## M2 backlog (found by breaking things)

- [ ] Missing symbol saved as null → `@NotBlank` + `@Valid`; `@Column(nullable = false)`
- [ ] Negative quantity accepted → `@Positive`
- [ ] Error bodies have no message → `@ControllerAdvice` + `ProblemDetail` (replace `@ResponseStatus` on exception)
- [ ] Data lost on restart → Postgres in docker-compose; `ddl-auto` not create-drop
- [ ] Price accepted as string `"90.90"` (lenient Jackson) — decide if strict
- [ ] Optional polish: `ResponseEntity.ok(...)` / `HttpStatus.CREATED` instead of `HttpStatusCode.valueOf(...)`

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

---

## How we work

- I (Chaitanya) write the code; Claude gives hints first, reviews on "check now".
- Fading help: M1 I say what's next; M2 I write the class list first; M3+ I design alone.
- Rules: type don't paste, one goal per session, commit every session, break it on purpose.
