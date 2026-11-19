# How TradeFlow runs on Kubernetes — one order's journey

Companion to `kubernetes-commands.md`. Diagrams use Mermaid (renders on GitHub
and in IntelliJ's Markdown preview); the ASCII map below works everywhere.

---

## 1. The map

```mermaid
flowchart TB
    PM["Postman on your Mac<br/>localhost:8090"]

    subgraph K8S["Kubernetes cluster · namespace tradeflow"]
        direction TB
        SVC_O["Service order-service<br/>type LoadBalancer<br/>port 8090 → targetPort 8080"]
        POD_O["POD order-service :8080<br/>controller · outbox publisher · listener"]

        SVC_P["Service postgres<br/>ClusterIP :5432"]
        POD_P[("POD postgres :5432")]

        SVC_K["Service kafka<br/>ClusterIP :29092"]
        POD_K[["POD kafka :29092<br/>topics: orders.created · orders.risk-decided"]]

        POD_R1["POD risk-service #1 :8081<br/>partitions 0,1"]
        POD_R2["POD risk-service #2 :8081<br/>partition 2"]
        POD_N["POD notification-service :8082"]
    end

    PM -->|"8090"| SVC_O --> POD_O
    POD_O -->|"postgres:5432"| SVC_P --> POD_P
    POD_O -->|"kafka:29092"| SVC_K --> POD_K
    POD_R1 -->|"kafka:29092"| SVC_K
    POD_R2 -->|"kafka:29092"| SVC_K
    POD_N -->|"kafka:29092"| SVC_K
```

Same picture in plain text:

```
 YOUR MAC
 ┌─────────────┐
 │  Postman    │  POST/GET http://localhost:8090/orders
 └──────┬──────┘
        │ 8090
 ═══════╪═══════════════ Kubernetes cluster (namespace: tradeflow) ═════════════════════
        ▼
  ┌──────────────────────────┐
  │ Service order-service    │  type LoadBalancer   port 8090 → targetPort 8080
  └────────────┬─────────────┘
               ▼ (finds pods with label app=order-service)
  ┌──────────────────────────┐        postgres:5432        ┌─────────────────────┐
  │ POD order-service  :8080 │ ──────────────────────────► │ Service postgres    │──► POD postgres :5432
  │  controller, outbox,     │                             └─────────────────────┘
  │  RiskDecidedListener     │        kafka:29092          ┌─────────────────────┐
  │                          │ ──────────────────────────► │ Service kafka       │──► POD kafka :29092
  └──────────────────────────┘                             └─────────────────────┘
                                                                   ▲      ▲
                                    kafka:29092 (they call Kafka)  │      │
  ┌──────────────────────────┐  ┌──────────────────────────┐       │      │
  │ POD risk-service #1 :8081│  │ POD risk-service #2 :8081│ ──────┘      │
  └──────────────────────────┘  └──────────────────────────┘              │
  ┌──────────────────────────────────┐                                     │
  │ POD notification-service  :8082  │ ────────────────────────────────────┘
  └──────────────────────────────────┘
```

**Arrows show who opens the connection.** Every arrow into Kafka starts at an app:
consumers *pull*, nobody pushes into them. That's why risk and notification have no Service.

---

## 2. How any internal call works (3 hops)

```mermaid
flowchart LR
    A["order-service pod<br/>connect to postgres:5432"] -->|"① who is 'postgres'?"| DNS["DNS (CoreDNS)<br/>phone book"]
    DNS -->|"② 10.110.168.136<br/>(Service IP)"| A
    A -->|"③ send to 10.110.168.136:5432"| KP["kube-proxy<br/>switchboard"]
    KP -->|"④ forward to a live pod<br/>with app=postgres"| P[("postgres pod<br/>10.1.0.8:5432")]
```

- **DNS = phone book.** Turns the Service name into the Service IP. Short name works inside the
  same namespace; full name is `postgres.tradeflow.svc.cluster.local`.
- **kube-proxy = switchboard.** The Service IP is not a machine; traffic to it is forwarded to one
  live pod with the matching label (picks one when there are several).
- Pods die and come back with **new IPs**; the Service name and IP **never change**.

---

## 3. POST /orders — create

```mermaid
sequenceDiagram
    autonumber
    participant PM as Postman (Mac)
    participant SO as Service order-service<br/>8090→8080
    participant O as order-service pod :8080
    participant PG as postgres:5432
    participant K as kafka:29092
    participant R as risk-service pods (×2)
    participant N as notification pod

    PM->>SO: POST localhost:8090/orders
    SO->>O: forward to pod :8080
    O->>PG: INSERT order + outbox row (same transaction)
    O-->>PM: 201 Created · PENDING_RISK
    Note over PM,O: HTTP part is over — the rest is inside the cluster

    loop every 1 s
        O->>PG: SELECT unsent outbox rows
        O->>K: send to orders.created (key = orderId)
        O->>PG: mark row sent
    end

    R->>K: poll orders.created (pod that owns the partition)
    R->>K: send RiskDecidedEvent to orders.risk-decided

    O->>K: poll orders.risk-decided (group order-service)
    O->>PG: check processed_events → status APPROVED / REJECTED
    N->>K: poll orders.risk-decided (group notification-service)
    Note over N: log "📩 Notify: order N APPROVED"
```

## 4. GET /orders/{id} — read

```mermaid
sequenceDiagram
    participant PM as Postman (Mac)
    participant SO as Service order-service<br/>8090→8080
    participant O as order-service pod :8080
    participant PG as postgres:5432

    PM->>SO: GET localhost:8090/orders/2
    SO->>O: forward to pod :8080
    O->>PG: SELECT order + items
    PG-->>O: row (APPROVED)
    O-->>PM: 200 OK · {"status":"APPROVED"}
```

Same door as the POST, plus one DB call. No Kafka involved.

---

## 5. Port table

| Who connects | Address used | Service port | → Pod port (targetPort) | Why |
|---|---|---|---|---|
| Postman (Mac) | `localhost:8090` | 8090 (LoadBalancer) | **8080** | the only door from outside |
| order-service → DB | `postgres:5432` | 5432 | 5432 | save / read orders |
| all 3 apps → Kafka | `kafka:29092` | 29092 | 29092 | send / read events |
| Kafka → its controller | `localhost:9093` | — | 9093 | same pod, so `localhost` |
| kubelet → health probes | pod IP `:8080 / :8081 / :8082` | — | direct | probes skip the Service |

Ports change **only at a Service** (`port` → `targetPort`). Probes go straight to the pod's
own IP, which is how risk and notification get health-checked without a Service.

## 6. Service types (which door?)

| Type | Door from outside? | TradeFlow |
|---|---|---|
| ClusterIP (default) | none — pod to pod only | postgres, kafka |
| NodePort | fixed port 30000–32767 on the node | (created under our LoadBalancer: `8090:31415`) |
| LoadBalancer | real external address (Docker Desktop: `localhost`) | order-service on 8090 |
| Ingress (separate object) | one entry for many services by path/domain | not used — real-world default |

---

## 7. Five sentences to remember

1. From outside there is **one door**: the order-service LoadBalancer, `localhost:8090` → pod `8080`.
2. Inside, everyone calls **by Service name** (`postgres:5432`, `kafka:29092`): DNS → Service IP → switchboard → live pod.
3. **Kafka consumers are never called**; they connect out and pull, so risk and notification need no Service.
4. Pods come and go with **new IPs**; Service names never change, so nobody uses pod IPs.
5. Ports change **only at the Service** (`port` → `targetPort`); probes go straight to the pod.
