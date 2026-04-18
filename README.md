# GigMatch — Real-Time Professional Services Dispatch Engine

---

## Project Overview

GigMatch is a **real-time dispatch platform for on-demand professional services**. Businesses and product teams post scoped job requests; pre-vetted **service companies** — dev shops, design studios, data consultancies, generalist agencies — have integrated their availability APIs with the platform. When a matching job arrives, their automated systems respond with a quote and estimated delivery time. GigMatch runs a quality-score-adjusted second-price auction across all responding companies in under 150ms and returns the best match.

This project demonstrates:
- **Java 21 async concurrency** — `CompletableFuture` fan-out with urgency-adjusted deadline (70–150ms)
- **Second-price auction mechanics** — winner selected by quality score; clearing price set by runner-up
- **ML integration** — Python trains a logistic regression model; coefficients baked into the Java service at startup
- **Redis atomic operations** — Lua scripts for race-condition-free token bucket capacity pacing
- **Angular 17+ reactive UI** — standalone components, signals, live-polling match log
- **Full observability** — Micrometer → Prometheus → Grafana with pre-built dashboard
- **Docker Compose** — single command brings up all 6 services

---

## Architecture

```
Client → POST /api/v1/matches
           │
           └─▶ MatchingEngine
                  │  1. Find eligible companies (ProviderRegistry.isEligible)
                  │  2. Filter by capacity (Redis token bucket — read-only check)
                  │  3. Deduplication (Redis setIfAbsent — 24h TTL per client+company)
                  │  4. Fan-out to 5 company dispatch systems (CompletableFuture, urgency-adjusted deadline)
                  │  5. Quality score: predictedSuccess × competitiveness
                  │  6. Second-price auction (winner pays runner-up's quote)
                  │  7. Atomic capacity deduction (Redis Lua script)
                  │  8. Async DB writes — fire-and-forget on dedicated thread pool
                  │  9. Record Prometheus metrics
                  └─▶ MatchResult { hasMatch, winnerProviderKey, clearingPriceCents, ... }
```

### Thread Pools

| Pool | Bean name | Core/Max | Purpose |
|------|-----------|----------|---------|
| `bidderExecutor` | `@Qualifier("bidderExecutor")` | 20/50 | Runs all 5 provider `bid()` calls in parallel |
| `dbWriteExecutor` | `@Qualifier("dbWriteExecutor")` | 5/10 | Fire-and-forget match log + activeJobs writes |

Keeping company dispatch simulations on a dedicated pool prevents the slowest responder (DataScienceProvider, 50–90ms) from starving faster companies on the common ForkJoinPool.

---

## How the Auction Works

### 1. Quality Score (ML-backed)

Each provider bid is adjusted by a predicted job success rate:

```
successRate     = sigmoid(1.8 × completionRate + 1.4 × (avgRating/5) + 0.9 × skillsMatch + intercept)
competitiveness = (clientBudget − providerQuote) / clientBudget   [clamped to 0–1]
effectiveScore  = successRate × competitiveness
```

`successRate` comes from a **logistic regression** trained on 10,000 synthetic job completion records. Coefficients are stored in `backend/src/main/resources/quality_score_coefficients.json` and loaded at startup by `QualityScoreModel` via `@PostConstruct`.

A provider quoting above the client's budget gets `competitiveness = 0` and is automatically excluded — no hard cutoff needed.

### 2. Second-Price Auction

- **Winner** = highest `effectiveScore` (best quality-per-dollar)
- **Clearing price** = runner-up's raw `quoteCents` (not their effectiveScore)
- If only one bidder, they pay their own quote

This separates *who wins* (quality-adjusted score) from *what is paid* (market-clearing rate). A high-quality provider with a modest quote can beat a cheaper but unreliable one — and the client pays only the next-best price, incentivising truthful bidding.

### 3. Token Bucket Capacity Pacing (Redis)

Each service company has a `dailyJobCapacity` — the maximum number of new contracts it can onboard in a day without degrading quality. The bucket refills continuously:

```
refillRate = dailyJobCapacity / 86_400_000   (slots per millisecond)
```

Every match atomically deducts 1 token via a Lua script. The check-and-deduct is a single Lua operation on Redis, making it race-condition-free under concurrent load. If the bucket is empty, the company is at capacity and excluded from the auction until slots refill.

### 4. Urgency-Adjusted Bid Deadline

`urgencyLevel` directly affects how long GigMatch waits for company dispatch systems to respond before closing the auction:

| Urgency | Effective deadline | Rationale |
|---------|--------------------|-----------|
| `HIGH` | `timeoutMs × 1.5` (default: 150ms) | Maximise company participation — urgent jobs deserve the widest provider pool |
| `MEDIUM` | `timeoutMs × 1.0` (default: 100ms) | Standard tradeoff |
| `LOW` | `timeoutMs × 0.7` (default: 70ms) | Quick-and-good-enough — optimise for speed; the client isn't in a rush |

The DataScienceProvider simulates 50–90ms response time. On `LOW` urgency with a 70ms deadline, it will occasionally not respond in time and be excluded — the auction proceeds with whoever replied. On `HIGH` urgency with a 150ms deadline, it always responds and participates.

### 5. Deduplication

A Redis key `seen:{clientId}:{providerId}` with a 24-hour TTL prevents the same company from being matched to the same client repeatedly in a short window — ensuring clients see variety rather than the same dominant agency every time.

---

## The 5 Integrated Service Companies

Each service company is modelled as a Spring `@Service` bean implementing `BidderService` — representing a real company's automated dispatch API. When a job request arrives, each eligible company's system evaluates it and responds with a quote and estimated delivery. They run asynchronously on `bidderExecutor` with simulated realistic response latencies.

| Company | Specialisation | Quote range | Rating | Completion | Response latency |
|---------|---------------|-------------|--------|-----------|-----------------|
| `ExpertDevProvider` | Software dev shop (backend, API) | $150–$300 | 4.9 | 97% | 20–60ms |
| `DesignProProvider` | Design studio (branding, UI/UX) | $80–$200 | 4.7 | 94% | 10–40ms |
| `GeneralistProvider` | Full-service agency (writing, admin, general) | $50–$150 | 4.2 | 88% | 5–30ms |
| `BudgetProvider` | High-volume agency (writing, general) | $20–$80 | 3.8 | 82% | 5–20ms |
| `DataScienceProvider` | Data consultancy (ML, analytics) | $200–$500 | 5.0 | 99% | 50–90ms |

`GeneralistProvider` and `BudgetProvider` have broad mandates — they respond to all job categories (equivalent to empty `targetCategories()` in the `BidderService` interface).

**Token bucket = daily onboarding capacity:** Each company has a `dailyJobCapacity` limit modelling the real constraint that a company can only take on so many new contracts per day before quality degrades. The Redis token bucket refills continuously across the day. Once exhausted, the company is excluded from auctions until capacity refills.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.2 |
| Async | CompletableFuture, dedicated thread pools |
| Database | PostgreSQL 16 (Flyway migrations) |
| Cache | Redis 7 (Lua scripts for atomic capacity pacing) |
| ML | Python 3.11, scikit-learn (logistic regression) |
| Frontend | Angular 17+ (standalone components, signals) |
| Metrics | Micrometer → Prometheus → Grafana |
| Load test | Python (aiohttp, asyncio, Rich) |
| Deploy | Docker Compose (6 services, one command) |

---

## Quick Start

### Prerequisites
- Docker + Docker Compose v2

```bash
cd gigmatch

# Start everything (Postgres, Redis, Backend, Frontend, Prometheus, Grafana)
docker compose up --build

# Wait ~60s for all services to become healthy
```

> **Windows users:** Use `http://127.0.0.1` instead of `http://localhost` if URLs don't load.
> This is a known Windows DNS issue where `localhost` can resolve to `::1` (IPv6) rather than `127.0.0.1`.

| Service | URL |
|---------|-----|
| Angular dashboard | http://127.0.0.1 |
| Backend API | http://127.0.0.1:8080 |
| Grafana (admin/admin) | http://127.0.0.1:3000 |
| Prometheus | http://127.0.0.1:9090 |

### Smoke Test

```bash
curl -X POST http://127.0.0.1:8080/api/v1/matches \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "test-001",
    "clientId": "client-1",
    "jobTitle": "Build REST API",
    "jobCategory": "software-dev",
    "requiredSkills": ["backend", "java"],
    "budgetCents": 30000,
    "urgencyLevel": "HIGH"
  }'
```

Expected response:
```json
{
  "hasMatch": true,
  "winnerProviderKey": "expert-dev",
  "clearingPriceCents": 18500,
  "predictedSuccessRate": 0.87,
  "effectiveScore": 0.41,
  "matchDurationMs": 63,
  "outcome": "MATCHED"
}
```

---

## Running Locally (without Docker)

```bash
# 1. Start Postgres + Redis
docker run -d -p 5432:5432 -e POSTGRES_DB=gigmatch -e POSTGRES_USER=gigmatch -e POSTGRES_PASSWORD=gigmatch postgres:16-alpine
docker run -d -p 6379:6379 redis:7-alpine

# 2. (Optional) Regenerate ML coefficients
cd ml
pip install -r requirements.txt
python generate_data.py && python train_model.py
# Auto-copies coefficients to backend/src/main/resources/

# 3. Start backend
cd backend
mvn spring-boot:run

# 4. Start frontend dev server
cd frontend
npm install
npm start    # → http://localhost:4200
```

---

## Running Tests

```bash
cd backend

# Unit tests only (no Docker required) — pure logic tests for auction, scoring, ML model
mvn test

# Unit + integration tests (requires Docker for Testcontainers)
# Spins up real Postgres + Redis containers for each test run
mvn verify
```

### Test Coverage

| Test class | Type | What it verifies |
|-----------|------|-----------------|
| `MatchingAuctionTest` | Pure unit | Second-price logic, quality flip winner, clearing price correctness |
| `QualityScoreModelTest` | Pure unit | Sigmoid bounds, monotonicity with better features |
| `CapacityPacerTest` | Testcontainers Redis | Token deduction, exhaustion, 20-thread concurrent deduction atomicity |
| `MatchEndpointIT` | Testcontainers full stack | End-to-end POST /matches with real Postgres + Redis |

---

## Load Testing

```bash
cd load-test
pip install -r requirements.txt
python load_test.py --rps 200 --duration 30 --url http://127.0.0.1:8080
```

Target: **p99 < 200ms** at 200 RPS on a laptop. The 100ms fan-out deadline means even if one slow provider (DataScienceProvider, 50–90ms) times out, the auction proceeds with whoever responded in time.

Sample output:
```
GigMatch Load Test
  URL:         http://127.0.0.1:8080
  Target RPS:  200
  Duration:    30s

┌─────────────────────────────┬──────────────┐
│ Metric                      │        Value │
├─────────────────────────────┼──────────────┤
│ Requests sent               │         6000 │
│ Actual RPS                  │        199.8 │
│ p50 latency                 │       42.3ms │
│ p95 latency                 │       89.1ms │
│ p99 latency                 │      134.6ms │
│ MATCHED                     │         4812 │
│ NO_MATCH                    │          188 │
│ Error rate                  │         0.0% │
└─────────────────────────────┴──────────────┘

✓ p99 134ms < 200ms target — PASS
```

---

## API Reference

### Match Endpoint

#### `POST /api/v1/matches`
Trigger a real-time auction for a job request. Always returns HTTP 200 — check `hasMatch` in the body.

**Request body:**
```json
{
  "requestId":      "uuid-string",
  "clientId":       "client-identifier",
  "jobTitle":       "Build a REST API",
  "jobCategory":    "software-dev",
  "requiredSkills": ["backend", "java"],
  "budgetCents":    30000,
  "urgencyLevel":   "HIGH"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `requestId` | string | yes | Caller-supplied idempotency key |
| `clientId` | string | yes | Used for deduplication (24h window) |
| `jobCategory` | string | yes | `software-dev`, `design`, `data-science`, `writing`, `general` |
| `requiredSkills` | string[] | no | Used for skill-match scoring |
| `budgetCents` | integer | yes | Client's maximum budget in cents |
| `urgencyLevel` | string | no | `LOW` (70ms deadline), `MEDIUM` (100ms), `HIGH` (150ms) — adjusts how long to wait for company systems |

**Response:**
```json
{
  "requestId":            "uuid-string",
  "hasMatch":             true,
  "winnerProviderKey":    "expert-dev",
  "winnerProviderId":     1,
  "clearingPriceCents":   18500,
  "predictedSuccessRate": 0.87,
  "effectiveScore":       0.413,
  "estimatedDays":        5,
  "matchDurationMs":      63,
  "outcome":              "MATCHED",
  "participatingProviders": ["expert-dev", "generalist", "budget"]
}
```

`outcome` values: `MATCHED` | `NO_MATCH` (no eligible companies or all quotes above budget) | `TIMEOUT` (all company systems exceeded the urgency-adjusted deadline: HIGH=150ms, MEDIUM=100ms, LOW=70ms)

---

#### `GET /api/v1/match-logs`
Paginated auction history, newest first.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | 0 | Page index (0-based) |
| `size` | 20 | Page size (max 100) |
| `providerId` | — | Filter by winning provider ID |

---

### Provider Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/providers` | List all providers with skill categories and portfolio summaries |
| `POST` | `/api/v1/providers` | Create a new provider |
| `GET` | `/api/v1/providers/{id}` | Provider detail |
| `PUT` | `/api/v1/providers/{id}` | Update provider metadata |
| `GET` | `/api/v1/providers/{id}/stats` | Win rate, total earnings, capacity remaining |
| `POST` | `/api/v1/providers/{id}/portfolios` | Upload a portfolio item |
| `GET` | `/api/v1/providers/summary` | Platform-wide totals (total matches, earnings, active providers) |
| `GET` | `/actuator/health` | Spring Boot health check |
| `GET` | `/actuator/prometheus` | Prometheus metrics scrape endpoint |

---

## Observability

### Prometheus Metrics

| Metric | Type | Labels | Description |
|--------|------|--------|-------------|
| `gigmatch_match_duration_ms` | Timer (histogram) | — | End-to-end auction latency — exposes p50/p95/p99 |
| `gigmatch_matches_total` | Counter | `outcome` | Total auctions by outcome (MATCHED / NO_MATCH / TIMEOUT) |
| `gigmatch_clearing_price_cents` | Distribution summary | — | Distribution of clearing prices |
| `gigmatch_provider_capacity_remaining` | Gauge | `providerId`, `providerKey` | Current token bucket level per provider |

### Grafana Dashboard

The pre-provisioned dashboard (`observability/grafana/provisioning/dashboards/gigmatch.json`) loads automatically at `http://127.0.0.1:3000`.

| Panel | Type | What it shows |
|-------|------|--------------|
| Total Matches / min | Stat | Rolling 1-minute match rate |
| Win Rate % | Stat | MATCHED / total over 5 minutes |
| p99 Match Latency | Stat | Green < 100ms, yellow < 200ms, red ≥ 200ms |
| Error Rate | Stat | (NO_MATCH + TIMEOUT) / total |
| Match Latency Percentiles | Time series | p50/p95/p99 lines over time |
| Matches/sec by Outcome | Time series | Stacked MATCHED / NO_MATCH / TIMEOUT |
| Clearing Price Distribution | Histogram | Price spread across all matches |
| Provider Capacity Remaining | Bar gauge | Per-provider token bucket level |

---

## Database Schema

Five Flyway migrations run automatically on startup:

| Migration | Table | Purpose |
|-----------|-------|---------|
| `V1` | `providers` | Provider profiles (rating, completion rate, capacity) |
| `V2` | `portfolios` | Work samples linked to providers |
| `V3` | `skill_offerings` | Per-provider skill categories with quote ranges |
| `V4` | `match_logs` | Immutable auction history |
| `V5` | *(seed data)* | 5 providers + skill offerings + portfolios matching the 5 beans |

---

## Project Structure

```
gigmatch/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml                                    Spring Boot 3.2.5, Java 21
│   └── src/
│       ├── main/
│       │   ├── java/com/gigmatch/
│       │   │   ├── GigMatchApplication.java
│       │   │   ├── config/
│       │   │   │   ├── AsyncConfig.java            bidderExecutor + dbWriteExecutor thread pools
│       │   │   │   ├── RedisConfig.java            RedisTemplate + two Lua script beans
│       │   │   │   └── WebConfig.java              CORS for local dev
│       │   │   ├── engine/
│       │   │   │   ├── MatchingEngine.java         Full auction orchestration (steps 1–9)
│       │   │   │   ├── MatchingAuction.java        Pure second-price auction logic
│       │   │   │   ├── ScoredBid.java              (bid, successRate, competitiveness, effectiveScore)
│       │   │   │   └── AuctionWinner.java          (winner ScoredBid, clearingPriceCents)
│       │   │   ├── provider/
│       │   │   │   ├── BidderService.java          Interface: getProviderKey, bid(JobRequest)
│       │   │   │   ├── ProviderRegistry.java       Holds all BidderService beans, O(1) lookup
│       │   │   │   ├── Provider.java               JPA entity
│       │   │   │   ├── ProviderRepository.java
│       │   │   │   ├── ProviderController.java     CRUD + stats + portfolio endpoints
│       │   │   │   ├── ProviderAdminService.java
│       │   │   │   └── impl/
│       │   │   │       ├── AbstractBidder.java     Base: latency sim, quote gen, portfolio lookup
│       │   │   │       ├── ExpertDevProvider.java
│       │   │   │       ├── DesignProProvider.java
│       │   │   │       ├── GeneralistProvider.java
│       │   │   │       ├── BudgetProvider.java
│       │   │   │       └── DataScienceProvider.java
│       │   │   ├── pacing/
│       │   │   │   ├── CapacityPacer.java          Redis token bucket (hasCapacity, deductCapacity)
│       │   │   │   └── PacingService.java          Redis deduplication (setIfAbsent, 24h TTL)
│       │   │   ├── score/
│       │   │   │   └── QualityScoreModel.java      Loads JSON coefficients, exposes predictSuccess()
│       │   │   ├── skill/
│       │   │   │   ├── SkillOffering.java
│       │   │   │   ├── SkillOfferingRepository.java
│       │   │   │   └── SkillMatchService.java      Computes fraction of required skills covered
│       │   │   ├── match/
│       │   │   │   ├── MatchController.java        POST /matches, GET /match-logs
│       │   │   │   ├── MatchLog.java               JPA entity (immutable audit record)
│       │   │   │   ├── MatchLogRepository.java
│       │   │   │   └── dto/
│       │   │   │       ├── JobRequest.java
│       │   │   │       ├── MatchResult.java
│       │   │   │       ├── ProviderBid.java
│       │   │   │       └── MatchLogDto.java
│       │   │   ├── metrics/
│       │   │   │   └── MatchMetrics.java           Timer + Counter + DistributionSummary + Gauge
│       │   │   ├── portfolio/
│       │   │   │   ├── Portfolio.java
│       │   │   │   ├── PortfolioRepository.java
│       │   │   │   └── dto/PortfolioDto.java
│       │   │   └── exception/
│       │   │       └── GlobalExceptionHandler.java  Maps 404/400/validation → ProblemDetail
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-docker.yml          Overrides DB/Redis hostnames for Docker
│       │       ├── quality_score_coefficients.json Trained logistic regression coefficients
│       │       └── db/migration/
│       │           ├── V1__create_providers.sql
│       │           ├── V2__create_portfolios.sql
│       │           ├── V3__create_skill_offerings.sql
│       │           ├── V4__create_match_logs.sql
│       │           └── V5__seed_data.sql
│       └── test/
│           └── java/com/gigmatch/
│               ├── engine/MatchingAuctionTest.java
│               ├── score/QualityScoreModelTest.java
│               ├── pacing/CapacityPacerTest.java    Testcontainers Redis, concurrency test
│               └── integration/MatchEndpointIT.java Full stack, Testcontainers Postgres + Redis
│
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf                                  SPA fallback + /api/ proxy to backend
│   ├── package.json                                Angular 17, Angular Material
│   ├── angular.json
│   ├── tsconfig.json
│   └── src/
│       └── app/
│           ├── app.component.ts                    MatSidenav shell
│           ├── app.config.ts                       provideRouter, provideHttpClient, provideAnimations
│           ├── app.routes.ts                       5 lazy-loaded standalone routes
│           ├── core/
│           │   ├── models/                         provider.model.ts, match-log.model.ts, stats.model.ts
│           │   └── services/                       provider.service.ts, match-log.service.ts
│           └── features/
│               ├── dashboard/                      4 stat cards + capacity bars + recent matches
│               ├── providers/
│               │   ├── provider-list/              MatTable with skill chips + capacity bars
│               │   ├── provider-form/              Reactive form with MatChipGrid for skills
│               │   └── provider-detail/            Stats + portfolio thumbnails + recent wins
│               └── match-log/                      Live-polling table (3s), paginated, outcome chips
│
├── ml/
│   ├── generate_data.py                            10,000 synthetic job completion rows
│   ├── train_model.py                              LogisticRegression → quality_score_coefficients.json
│   └── requirements.txt
│
├── load-test/
│   ├── load_test.py                                asyncio + aiohttp, Rich live display, p50/p95/p99
│   └── requirements.txt
│
├── observability/
│   ├── prometheus.yml                              Scrapes /actuator/prometheus every 5s
│   └── grafana/provisioning/
│       ├── datasources/prometheus.yml              Auto-wires Prometheus datasource
│       └── dashboards/
│           ├── dashboard.yml
│           └── gigmatch.json                       8-panel pre-built dashboard
│
├── docker-compose.yml
└── README.md
```

---

## Key Design Decisions

### Money in integer cents
All monetary values (`budgetCents`, `quoteCents`, `clearingPriceCents`) are `int` throughout — stored in the database, serialised in JSON, and used in arithmetic. This avoids floating-point precision errors for financial comparisons.

### Quality score selects winner; runner-up price sets clearing price
`effectiveScore` determines *who wins*. The clearing price is the runner-up's raw `quoteCents`. This is the Vickrey second-price mechanism: providers are incentivised to bid their true value because they cannot gain by bidding lower (they might lose) or higher (they pay more than necessary).

### Providers with quotes above budget are silently excluded
`competitiveness = max(0, (budget - quote) / budget)` — a quote above budget yields `competitiveness = 0`, so `effectiveScore = 0`. The auction naturally excludes them without any explicit cutoff logic.

### Async fire-and-forget writes
`MatchLog` save and `activeJobs` counter increment are dispatched on `dbWriteExecutor` after the winner is selected — they are never awaited. The client receives the `MatchResult` as soon as the auction completes, keeping P99 latency low even when Postgres is momentarily slow.

### Redis Lua for atomic capacity deduction
A single Lua script reads the token bucket state, computes refill, checks if tokens >= 1, and deducts — all atomically. This prevents race conditions under concurrent load where two requests for the same provider could both pass the `hasCapacity` check and over-allocate.

### Fail-open on Redis errors
Both `hasCapacity` and `deductCapacity` catch Redis exceptions and return `true` (allow). This trades precision for availability — a Redis outage degrades capacity tracking gracefully instead of taking down all matching.

### No authentication
Intentionally omitted to keep the codebase focused on the auction mechanics. In production, you'd add JWT/OAuth2 at the Spring Security layer.

---

## ML Model

The quality score model lives in `ml/` and can be retrained at any time:

```bash
cd ml
pip install -r requirements.txt

# Generate 10,000 synthetic training rows
python generate_data.py

# Train logistic regression, print accuracy, write coefficients JSON,
# and copy it to backend/src/main/resources/
python train_model.py
```

**Features:**

| Feature | Description | Range |
|---------|-------------|-------|
| `completion_rate` | Historical fraction of jobs completed | 0.0–1.0 |
| `avg_rating_norm` | Average client rating, normalised (rating / 5.0) | 0.0–1.0 |
| `skills_match_score` | Fraction of required skills covered by provider | 0.0–1.0 |

**Default trained coefficients** (`quality_score_coefficients.json`):
```json
{
  "coefficients": {
    "completion_rate":   1.8,
    "avg_rating_norm":   1.4,
    "skills_match_score": 0.9
  },
  "intercept": -2.3
}
```

The `QualityScoreModel` bean reads this file from the classpath at startup via `@PostConstruct`, so no restart is needed after retraining — just rebuild the Docker image.
