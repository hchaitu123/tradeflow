# Kubernetes command cheat sheet (TradeFlow, M6)

Cluster: Docker Desktop → Settings → Kubernetes → **kubeadm** (shares Docker's images).
Everything lives in the namespace `tradeflow`, so most commands need `-n tradeflow`.

## Picture

```
Mac localhost:8090 ──LoadBalancer──► Service order-service ──label app=order-service──► pod(s)
                                       ▲ stable name + port         ▲ kept alive by the Deployment
inside the cluster: order-service → postgres:5432, kafka:29092 (Service names)
```

| Object | Job | Simple picture |
|---|---|---|
| Namespace | separate area for our things | a floor in the building |
| Deployment | keep N pods of this image running | the manager |
| Pod | one running copy of the app | the chef |
| Service | stable name + port, finds pods by label | the restaurant's phone number |
| ConfigMap | settings given to pods as env vars | the SIM card |

## Start / stop everything

```bash
kubectl config use-context docker-desktop
```
```bash
docker compose build                      # rebuild images after code changes
```
```bash
kubectl apply -f k8s/                      # create/update everything in the folder
```
```bash
kubectl delete namespace tradeflow         # remove everything (the namespace too)
```

## Look around

```bash
kubectl get pods,svc,cm -n tradeflow
```
```bash
kubectl get pods -n tradeflow -w           # watch live (Ctrl+C to stop)
```
```bash
kubectl get pods -n tradeflow -o wide      # + pod IPs
```
```bash
kubectl describe pod -n tradeflow -l app=order-service   # events: pull errors, probe failures
```

## Logs and inside the container

```bash
kubectl logs -n tradeflow deploy/order-service --tail=20
```
```bash
kubectl logs -n tradeflow -l app=risk-service --tail=200 | grep "partitions assigned"   # all pods with that label
```
```bash
kubectl exec -n tradeflow deploy/order-service -- printenv | grep SPRING
```
```bash
kubectl exec -n tradeflow deploy/kafka -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list
```

## Reach it from the Mac

order-service is a **LoadBalancer** Service on port 8090 → Docker Desktop opens `localhost:8090`
permanently (Postman: `http://localhost:8090/orders`). Full flow + diagrams: `kubernetes-flow.md`.

```bash
curl -s -X POST localhost:8090/orders -H "Content-Type: application/json" -d '{"accountId":1,"items":[{"symbol":"INFY","quantity":10,"price":1500.00,"side":"BUY"}]}'
```
Other Services are ClusterIP (inside only) → temporary gate, left = Mac, right = Service:
```bash
kubectl port-forward -n tradeflow svc/postgres 5434:5432   # DB from DBeaver/psql (5433 may be Compose's)
```
"address already in use" → find who holds the port: `lsof -nP -iTCP:8090 -sTCP:LISTEN`.
Allowed symbols: INFY, TCS, RELIANCE, HDFCBANK, ITC (anything else → REJECTED).

## Self-healing, scaling, restarts

```bash
kubectl delete pod -n tradeflow -l app=postgres            # Deployment makes a new one
```
```bash
kubectl scale deploy/risk-service -n tradeflow --replicas=2   # quick; but the YAML is the source of truth
```
```bash
kubectl rollout restart deploy/order-service -n tradeflow   # needed after a ConfigMap change or image rebuild
```
```bash
kubectl rollout status deploy/order-service -n tradeflow
```

## Gotchas we hit

- `kind:` is the object type (Namespace, Deployment…), not "Kubeadm".
- Saving a YAML file changes nothing; `kubectl apply` does.
- No `-n tradeflow` → you're looking at `default` → "No resources found".
- Env `value:` must be a string: `"1"`, not `1`.
- `:latest` + no `imagePullPolicy: Never` → `ErrImagePull` (tries Docker Hub).
- Service named `kafka` → `enableServiceLinks: false`, or the Kafka image reads the injected `KAFKA_PORT…` vars as config.
- YAML indentation decides ownership (a probe one level too deep belongs to the other probe).
- Rebuilt the image with the same `:latest` tag? Pods keep the old one until `rollout restart`.
- `port-forward` dies when its pod is replaced; just run it again.
- Don't scale order-service: every pod runs the outbox publisher on the same rows → duplicate events.
