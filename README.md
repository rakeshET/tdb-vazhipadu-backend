# tdb-vazhipadu-backend

Vazhipadu (temple offering) booking service for a temple board. Devotees book offerings such as
Pushpanjali or Archana for a date. The server prices every order.

## Stack

- Java 17, Spring Boot 3.3 (Web, Data JPA, Validation)
- H2 in-memory database, schema managed by Flyway (`src/main/resources/db/migration`)
- JUnit 5, Mockito, AssertJ, MockMvc

## Run

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Data is in memory and reset on restart.

## Test

```bash
mvn clean verify
```

## Project structure

```
com.tdb.vazhipadu
├── config        Clock (IST) and typed application properties
├── controller    REST endpoints, thin; delegate to services
├── service       Business rules and transactions
├── repository    Spring Data JPA repositories
├── entity        JPA entities and enums
├── dto           request/ and response/ records
├── mapper        Entity → response DTO mapping
└── exception     Error codes, ApiException and the global JSON error handler
```

## API

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/temples` | Create a temple |
| GET | `/api/temples/{templeId}` | Get a temple |
| POST | `/api/temples/{templeId}/offerings` | Add an offering (name, price, active, optional dailyLimit) |
| GET | `/api/temples/{templeId}/offerings` | List a temple's offerings |
| GET | `/api/temples/{templeId}/offerings/{offeringId}` | Get an offering |
| PUT | `/api/temples/{templeId}/offerings/{offeringId}` | Update an offering, including activating or deactivating it |
| POST | `/api/temples/{templeId}/orders` | Place an order; the server computes the total |
| GET | `/api/temples/{templeId}/orders` | Search orders by `date`, `status`, `phone`; paged with `page` (0-based) and `size` (1–100, default 20) |
| GET | `/api/temples/{templeId}/orders/{orderId}` | Get an order by id |

### Example

```bash
curl -X POST localhost:8080/api/temples -H 'Content-Type: application/json' \
  -d '{"name":"Sree Padmanabhaswamy"}'

curl -X POST localhost:8080/api/temples/1/offerings -H 'Content-Type: application/json' \
  -d '{"name":"Pushpanjali","price":30}'
curl -X POST localhost:8080/api/temples/1/offerings -H 'Content-Type: application/json' \
  -d '{"name":"Archana","price":50}'

curl -X POST localhost:8080/api/temples/1/orders -H 'Content-Type: application/json' -d '{
  "devoteeName": "Ravi",
  "phone": "9876543210",
  "nakshatra": "ROHINI",
  "offeringDate": "2026-11-01",
  "items": [{"offeringId": 1, "quantity": 2}, {"offeringId": 2, "quantity": 1}]
}'
# -> 201 Created, "totalAmount": 110.00

curl 'localhost:8080/api/temples/1/orders?phone=9876543210&status=CONFIRMED&page=0&size=20'
# -> {"content": [...], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "hasNext": false}
```

### Order search

- Results are always sorted by order id ascending. Ids only grow, so new orders land at the end and
  earlier pages never shift; page *n+1* continues exactly where page *n* stopped.
- Every filter is optional and filters combine with AND. `phone` is an exact 10-digit match.
- Indexes `(temple_id, offering_date, id)`, `(temple_id, status, id)` and `(temple_id, phone, id)` serve
  the common searches and their sort without scanning the table. `OrderRepositorySearchTest` asserts this
  with `EXPLAIN`.

### Daily limits

- An offering may set `dailyLimit` (e.g. 50 Udayasthamana Poojas). Omitting it, or sending `null`, means unlimited.
- An order is rejected with `409 DAILY_LIMIT_EXCEEDED` when the confirmed quantity for that offering on the
  offering date plus the requested quantity would exceed the limit. Cancelled orders don't count.
- Simultaneous orders are serialised per limited offering with a `SELECT … FOR UPDATE` row lock, taken in
  offering-id order so multi-offering orders cannot deadlock. Offerings without a limit are never locked.
  If the lock can't be acquired within 10 s, the API returns `503 TRY_AGAIN`.

### Error format

Every error uses the same JSON shape:

```json
{
  "timestamp": "2026-10-27T03:30:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/api/temples/1/orders",
  "errors": [{"field": "phone", "message": "must be a 10-digit Indian mobile number"}]
}
```

| Status | Codes |
|--------|-------|
| 400 | `VALIDATION_FAILED`, `MALFORMED_REQUEST` |
| 404 | `NOT_FOUND` |
| 409 | `DUPLICATE_OFFERING`, `DAILY_LIMIT_EXCEEDED`, `DATA_CONFLICT` |
| 422 | `OFFERING_INACTIVE`, `UNKNOWN_OFFERING` |
| 503 | `TRY_AGAIN` |

## Pull requests

| Problem | PR |
|---------|----|
| P1 – Offerings and Orders API | [#5](https://github.com/rakeshET/tdb-vazhipadu-backend/pull/5) |
| P2 – Order Search | [#6](https://github.com/rakeshET/tdb-vazhipadu-backend/pull/6) |
| P3 – Daily Limits | _pending_ |
| P4 – Cancellation and Daily Collection | _pending_ |
