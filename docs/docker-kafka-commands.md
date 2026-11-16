# Docker + Kafka — every command we ran, explained

Run all commands from the `tradeflow` root (where `docker-compose.yml` lives).

---

## 0. Words you need first

| Term | Meaning |
|---|---|
| **Image** | A read-only template: OS files + installed software. e.g. `postgres:17`, `apache/kafka:4.1.0`. Like a class. |
| **Container** | A running copy of an image, with its own files, network and processes. Like an object made from the class. |
| **Tag** | The part after `:` in an image name, usually the version (`postgres:17`). Always pin it — `latest` can change under you. |
| **Port mapping** `HOST:CONTAINER` | `5433:5432` = a door on your Mac at 5433 leading to 5432 inside the container. The Mac side must be free. |
| **Volume** | Storage that lives **outside** the container, so data survives when the container is deleted. |
| **Compose** | A YAML file describing several containers together; one command starts them all. |
| **Service** (compose) | One entry under `services:` (`postgres`, `kafka`). Also becomes its hostname inside Docker's network. |
| **Host** | Your Mac. Apps run from IntelliJ are on the host → they use `localhost:<host port>`. |

---

## 1. docker-compose.yml — what each line means

```yaml
services:
  postgres:                          # service name (hostname inside Docker network)
    image: postgres:17               # which image + version
    container_name: tradeflow-postgres   # fixed name, so we can type it in commands
    environment:                     # env vars the image reads on FIRST start
      POSTGRES_DB: tradeflow         #   create this database
      POSTGRES_USER: tradeflow       #   create this user
      POSTGRES_PASSWORD: tradeflow
    ports:
      - "5433:5432"                  # Mac 5433 → container 5432 (Homebrew Postgres already owns Mac 5432)
    volumes:
      - pgdata:/var/lib/postgresql/data   # named volume → the folder where Postgres 17 keeps its data

  kafka:
    image: apache/kafka:4.1.0        # official image; single-node KRaft (no ZooKeeper)
    container_name: tradeflow-kafka
    ports:
      - "9092:9092"                  # gate for the Mac → EXTERNAL listener
    environment:                     # setting ANY KAFKA_* replaces the image defaults → full config
      KAFKA_NODE_ID: 1                               # I am server no. 1
      KAFKA_PROCESS_ROLES: broker,controller         # broker + my own manager (KRaft)
      KAFKA_LISTENERS: EXTERNAL://:9092,INTERNAL://:29092,CONTROLLER://:9093   # doors opened inside the container
      KAFKA_ADVERTISED_LISTENERS: EXTERNAL://localhost:9092,INTERNAL://kafka:29092  # "call me back at…"
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: EXTERNAL:PLAINTEXT,INTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093   # voters: id@host:port (3 servers → 3 entries)
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1      # 1 server → 1 copy of internal topics
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_NUM_PARTITIONS: 3                        # auto-created topics get 3 partitions

  order-service:
    build:                           # build the image from our Dockerfile
      context: .                     #   folder Docker may COPY from (project root)
      args:
        SERVICE: order-service       #   fills ARG SERVICE in the Dockerfile
    image: tradeflow/order-service   # name for the built image
    container_name: tradeflow-order-service
    ports:
      - "8080:8080"                  # gate so curl from the Mac works
    environment:                     # env vars override application*.yaml
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tradeflow   # house name + inner port
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092                        # Kafka's INSIDE line
    depends_on: [postgres, kafka]    # start order only — NOT "wait until ready"
    restart: on-failure              # so a too-early start just retries

  # risk-service / notification-service: same, but no ports, no datasource, depends_on kafka only

volumes:
  pgdata:                            # declare the named volume (top level, NOT under services)
```

### Who uses which address (the "houses on a street" rule)
`localhost` = the machine/container **where the command runs**. Containers find each other by **service name**.

| Caller | Postgres | Kafka |
|---|---|---|
| Mac (IntelliJ, curl, psql on Mac) | `localhost:5433` | `localhost:9092` |
| Another container (compose apps) | `postgres:5432` | `kafka:29092` |
| `docker exec` inside that same container | `localhost:5432` | `localhost:9092` |

`ports: "A:B"` = gate: Mac port A → container port B. Only needed when the **Mac** must reach the container.

---

## 2. Docker Compose commands (manage the whole stack)

| Command | What it does | When |
|---|---|---|
| `docker compose up -d` | Create + start everything in the file. Already-running containers are **left alone** (Postgres stayed "Up 38 hours" when we added Kafka). | Start of day; after editing the file |
| `docker compose ps` | List this project's containers: name, image, status, ports. | "Is it running?" |
| `docker compose logs postgres` | Print a service's logs. Add `-f` to follow live. | Something failed to start |
| `docker compose down` | Stop **and delete** the containers. **Volume kept → data survives.** | End of work, free memory |
| `docker compose down -v` | Same, **plus delete volumes → all DB data gone.** | Reset to a clean DB (we did this before adding `@Version`) |

Flags:
- `-d` = **detached**: run in the background and give the terminal back. Without it, logs fill the terminal and Ctrl+C stops everything.
- `-v` = **volumes**: also remove the named volumes. ⚠️ destructive.

---

## 3. Docker commands (one container at a time)

| Command | What it does |
|---|---|
| `docker ps` | All running containers (any project). `docker ps -a` includes stopped ones. |
| `docker logs tradeflow-kafka` | Logs of one container. Add `-f` to follow, or pipe: `\| grep -i started`. |
| `docker stop tradeflow-kafka` | Stop it (container + data kept). We used this to break Kafka on purpose. |
| `docker start tradeflow-kafka` | Start a stopped container again. |
| `docker exec -it <container> <command>` | Run a command **inside** a running container. |

`docker exec` flags:
- `-i` = **interactive**: keep input open so you can type (needed for psql, the Kafka producer).
- `-t` = **tty**: give it a proper terminal (prompts, colours, Ctrl+C works).
- Together `-it` = "let me use it like a normal terminal".
- Everything after the container name is the command that runs inside it — type it all on **one line**.

---

## 4. Postgres via docker exec

```bash
docker exec -it tradeflow-postgres psql -U tradeflow -d tradeflow -c "select order_id, status from orders order by order_id desc limit 3;"
```

| Part | Meaning |
|---|---|
| `psql` | Postgres command-line client (already installed inside the image) |
| `-U tradeflow` | log in as **user** tradeflow |
| `-d tradeflow` | connect to **database** tradeflow |
| `-c "..."` | run this one SQL **command** and exit |

Leave out `-c "..."` to get an interactive `tradeflow=#` prompt. Useful inside it: `\dt` (list tables), `\d orders` (describe table), `\q` (quit).

---

## 5. Kafka CLI via docker exec

The Kafka tools live inside the container at `/opt/kafka/bin/`. Every command needs
`--bootstrap-server localhost:9092` = "which Kafka to talk to" (here `localhost` means *inside the container*).

### Topics
```bash
# create
docker exec -it tradeflow-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic orders.created --partitions 3 --replication-factor 1

# describe (partitions, leader, replicas, ISR)
docker exec -it tradeflow-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic orders.created

# list all topics
docker exec -it tradeflow-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

| Term | Meaning |
|---|---|
| **topic** | Named log of messages (`orders.created`, `orders.risk-decided`) |
| `--partitions 3` | Split into 3 logs → up to 3 consumers in a group read in parallel. Order kept **within** a partition only. |
| `--replication-factor 1` | Copies of each partition. 1 broker → 1. Prod usually 3. |
| **Leader** (describe) | Broker that handles reads/writes for that partition |
| **Isr** (describe) | In-Sync Replicas — copies fully caught up |
| `min.insync.replicas` | How many replicas must have a write before it counts (pairs with producer `acks=all`) |

### Produce by hand
```bash
docker exec -it tradeflow-kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic orders.created --property parse.key=true --property key.separator=:
```
Then type lines like `123:hello`. Ctrl+C to stop.

| Part | Meaning |
|---|---|
| `parse.key=true` | treat part of each line as the **key** |
| `key.separator=:` | text before the first `:` is the key, after it is the value |

### Consume / watch
```bash
docker exec -it tradeflow-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders.created --from-beginning --property print.key=true --property print.partition=true --property print.headers=true
```

| Part | Meaning |
|---|---|
| `--from-beginning` | read the whole topic, not just new messages |
| `print.key=true` | show each message's key |
| `print.partition=true` | show which partition it's in |
| `print.headers=true` | show headers (e.g. `__TypeId__` added by Spring's JSON serializer) |

### Consumer groups (useful next)
```bash
# where is each group up to? (offsets + LAG = messages not yet read)
docker exec -it tradeflow-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group risk-service

# replay: move a group back to the start (stop the app first)
docker exec -it tradeflow-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group risk-service --reset-offsets --to-earliest --topic orders.created --execute
```

| Term | Meaning |
|---|---|
| **consumer group** (`group-id`) | Copies with the same group **share** partitions; different groups **each get every message** |
| **offset** | A message's position number in a partition. Kafka stores, per group, how far it has read. |
| **LAG** | latest offset − group's offset = how far behind the consumer is |
| `auto-offset-reset: earliest` | Only used when a group has **no saved offset** — then start from the oldest message |

---

## 6. Building and running our own images (M5)

```bash
# build one image by hand
docker build --build-arg SERVICE=order-service -t tradeflow/order-service .
docker images | grep tradeflow

# build + start everything in docker-compose.yml
docker compose up -d --build

# rebuild/restart just one service (e.g. after a code change)
docker compose up -d --build order-service

# recreate Kafka after changing its settings (⚠️ wipes topics — no volume)
docker compose up -d kafka

# logs: one container, follow live (Ctrl+C stops watching, not the container)
docker logs -f tradeflow-order-service

# logs: several services merged, each line prefixed with its service name
docker compose logs order-service risk-service notification-service | grep -E "Outbox .* sent|→|Notify"

# check the compose file without running anything
docker compose config -q
```

| Part | Meaning |
|---|---|
| `--build-arg SERVICE=…` | value for `ARG SERVICE` in the Dockerfile |
| `-t name` | tag (name) the image |
| `.` (last arg) | build context = the folder Docker can `COPY` from (`.dockerignore` filters it) |
| `--build` | rebuild images before starting, so the latest code is inside |
| `-f` | follow logs live |
| `config -q` | validate compose syntax, print nothing if OK |

**Multi-stage Dockerfile:** stage 1 `FROM eclipse-temurin:21-jdk AS build` compiles (needs JDK); stage 2 `FROM eclipse-temurin:21-jre` only runs; `COPY --from=build` takes just the jar. Comments must be on their own line. `-DskipTests` because Testcontainers needs Docker, which isn't inside a build.

---

## 7. Helpers we used

| Command | What it does |
|---|---|
| `lsof -i :9092` | Who is using port 9092 on the Mac? (No output = free.) How we found Homebrew Postgres on 5432. |
| `docker exec ... \| grep ...` / `docker logs ... \| grep -i started` | Filter long output for the line you care about |

---

## 8. Gotchas we hit

- `volumes:` indented under `services:` → compose error. It must be top level.
- Port already taken on the Mac → change the **left** number only (`5433:5432`).
- "Unable to determine Dialect" was really **connecting to the wrong Postgres** (Homebrew on 5432).
- `docker exec` command split across lines / pasted hidden characters → zsh parse errors. Type it on one line.
- `down -v` wipes the DB. Plain `down` and `up -d` never do.
- Ignore "What's next: Try Docker Debug…" — Docker advertising.
- `FROM … AS build  # comment` → "FROM requires either one or three arguments". No trailing comments in Dockerfiles.
- Writing your **own** service name as the host (`order-service:5432`) = calling your own house. Use the target's name.
- Port 8080 already used by IntelliJ's order-service → container can't open its gate. Stop one of them.
- Same app running in IntelliJ **and** Docker with the same group-id → they split partitions.
- Container logs are UTC (`…Z`); Mac logs show `+05:30`.
