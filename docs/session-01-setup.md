# Session 1 — Setup (Mon 2 Nov 2026)

Simple reference of what I did and why. Read before next session.

---

## 1. What I have now

- A project folder `~/code/tradeflow` that is a Git repo, pushed to GitHub.
- One parent project (`tradeflow`) with one child service (`order-service`).
- `./mvnw verify` → BUILD SUCCESS for both.
- App starts and `http://localhost:8080/actuator/health` says `UP`.
- Working on branch `m1-order-service`.

```
tradeflow/
├── pom.xml              ← parent (just groups the services)
├── mvnw, mvnw.cmd, .mvn ← Maven wrapper
├── .gitignore, .gitattributes
├── docs/
└── order-service/
    ├── pom.xml          ← child (real code + dependencies)
    └── src/
```

---

## 2. Tools on my Mac

| Tool | Version | Note |
|---|---|---|
| Java | 21 (Temurin, via SDKMAN) | LTS version |
| Maven | wrapper uses 3.10.0 | |
| Spring Boot | 4.1.2 | latest major; old tutorials are for 2.x/3.x |
| Docker Desktop | 29.8 | must be **opened** before using `docker` |
| IntelliJ IDEA | 2026.2 (free tier) | Spring Initializr wizard is paid → use start.spring.io website |
| GitHub | account `hchaitu123`, SSH working | |

---

## 3. Key ideas in plain words

**Parent pom vs child pom**
- Parent (`tradeflow/pom.xml`): `<packaging>pom</packaging>`, lists `<modules>`, sets Java version and Spring Boot version once. Makes no jar.
- Child (`order-service/pom.xml`): has the dependencies and builds the actual jar. Its `<parent>` points to `tradeflow`.
- Chain: `spring-boot-starter-parent` → `tradeflow` → `order-service`.
- Why: one command builds all services, all share the same versions. Later I add `risk-service` and `notification-service` the same way.

**Maven wrapper (`mvnw`)**
- A small script that downloads the exact Maven version the project wants.
- Everyone (me, CI, another laptop) builds with the same Maven. No "works on my machine".
- Lives in the **root**, not inside a service.

**How Maven finds a parent pom**
1. Looks at `relativePath` (default = `../pom.xml`)
2. Then local cache `~/.m2`
3. Then the internet (Maven Central)
- Empty `<relativePath/>` = "skip disk, go to internet". OK for Spring's parent, wrong for my own parent.

**application.yaml**
- Config file for Spring. YAML = nested + readable.
- Spaces only, no tabs. Wrong indent = setting silently ignored.

**.gitignore / .gitattributes**
- `.gitignore`: keeps `target/` (build output) and `.idea/` (IDE files) out of Git.
- `.gitattributes`: keeps `mvnw` with Unix line endings so it doesn't break in CI.

**Docker**
- `docker` command is only the client. Docker Desktop runs the engine.
- Error "cannot connect … docker.sock" = Docker Desktop not running → open it.
- First `docker run` of an image downloads it; after that it's cached.

---

## 4. Problems I hit (and fixes)

| Problem | Cause | Fix |
|---|---|---|
| `docker.sock: no such file` | Docker Desktop not open | `open -a Docker`, wait for "Engine running" |
| `./mvnw: no such file` | wrapper still inside `order-service` | moved `mvnw`, `mvnw.cmd`, `.mvn` to root |
| `Non-resolvable parent POM … NOT allowed from central` | child pom had empty `<relativePath/>` | deleted that line → Maven looks at `../pom.xml` |

How to read a Maven error: skip the noise, find the `[FATAL]` / first `[ERROR]` line, read it in pieces, note the file + line number.

---

## 5. Commands I used

```bash
./mvnw -v                   # check Maven + Java version
./mvnw verify               # compile + run tests for all modules
git status                  # what changed / what's staged
git add .                   # stage everything (respects .gitignore)
git restore --staged <file> # un-stage (keeps the file)
git commit -m "message"
gh repo create tradeflow --public --source=. --push
git switch -c m1-order-service   # new branch
git push -u origin m1-order-service
```

Git workflow: `main` always green → one branch per milestone → merge + `git tag m1` when DONE.

---

## 6. Spring Boot 4 gotchas (for when tutorials don't work)

- `@MockBean` removed → use `@MockitoBean`.
- Starters/test slices are more split up → `@WebMvcTest` etc. may need an extra test dependency and a different import.
- JSON uses Jackson 3 (`tools.jackson...` imports).
- If tutorial code won't compile, check which Boot version it was written for.

---

## 7. Interview questions I can answer now

- What is a multi-module Maven project and why use one?
- Difference between parent pom and child pom? What does `packaging=pom` mean?
- Why use the Maven wrapper?
- How does Maven resolve a parent pom?
- What does `mvn verify` do? (compile → test → package → verify)

---

## 8. Next session — goal

**`POST /orders` and `GET /orders/{id}` working in Postman (H2).**

1. Design Order fields first: id, symbol, side (BUY/SELL), quantity, price (**BigDecimal**), status (NEW), createdAt.
2. Build bottom-up: Entity → Repository → DTO records → Service → Controller.
3. Test in Postman, then break it (bad side, missing field) and note what happens.
4. Commit + `git push -u origin m1-order-service`.

Be ready to answer: Why DTOs instead of returning the entity? Why BigDecimal not double? Why constructor injection?
