# tdb-vazhipadu-backend

Vazhipadu (temple offering) booking service for a temple board. Devotees book offerings such as
Pushpanjali or Archana for a date. The server prices every order.

## Stack

- Java 17, Spring Boot 3.3 (Web, Data JPA, Validation)
- H2 in-memory database
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
| POST | `/api/temples/{templeId}/offerings` | Add an offering (name, price, active) |
| GET | `/api/temples/{templeId}/offerings` | List a temple's offerings |
| GET | `/api/temples/{templeId}/offerings/{offeringId}` | Get an offering |
| PUT | `/api/temples/{templeId}/offerings/{offeringId}` | Update an offering, including activating or deactivating it |
| POST | `/api/temples/{templeId}/orders` | Place an order; the server computes the total |
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
```

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
| 409 | `DUPLICATE_OFFERING` |
| 422 | `OFFERING_INACTIVE`, `UNKNOWN_OFFERING` |

## Pull requests

| Problem | PR |
|---------|----|
| P1 – Offerings and Orders API | _pending_ |
| P2 – Order Search | _pending_ |
| P3 – Daily Limits | _pending_ |
| P4 – Cancellation and Daily Collection | _pending_ |
