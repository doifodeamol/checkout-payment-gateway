# Checkout Payment Gateway Challenge (Java)

A small Spring Boot payment gateway that validates merchant payment requests, sends valid card payments to the supplied acquiring-bank simulator, stores safe payment details in memory, and supports payment retrieval.

## Requirements

- JDK 17+
- Docker and Docker Compose (for the bank simulator)

## Run locally

Choose one of the following options.

### Option A: run both services with Docker Compose

From the project root:

```bash
docker compose up --build -d
```

This starts the bank simulator on `http://localhost:8080` and the gateway on `http://localhost:8090`. Check both containers with `docker compose ps`; inspect logs with `docker compose logs -f payment_gateway` or `docker compose logs -f bank_simulator`.

Stop the services with:

```bash
docker compose down
```

### Option B: run the gateway from Gradle

1. Start only the acquiring-bank simulator:

   ```bash
   docker compose up -d bank_simulator
   ```

2. In another terminal, start the gateway on the host:

   ```bash
   ./gradlew bootRun
   ```

The gateway listens on `http://localhost:8090`. The bank simulator listens on `http://localhost:8080`. The bank URL can be overridden with `BANK_BASE_URL`. When running the gateway on the host, the default URL is `http://localhost:8080`. When running both services through Compose, the gateway uses the Compose service name `bank_simulator`.

## API

### Process a payment

`POST /payment`

```json
{
  "card_number": "2222405343248877",
  "expiry_month": 12,
  "expiry_year": 2099,
  "currency": "GBP",
  "amount": 100,
  "cvv": "123"
}
```

`amount` is expressed in minor currency units (for example, GBP 1.00 is `100`). Supported currencies are deliberately limited to GBP, USD and EUR, as permitted by the exercise.

A payment accepted by the bank returns an identifier, status (`Authorized` or `Declined`), last four card digits, expiry month/year, currency and amount. Invalid requests return HTTP 400 with status `Rejected`. If the bank is unavailable, the gateway returns HTTP 503 and does not misrepresent the outcome as a decline.

### Retrieve a payment

`GET /payment/{id}`

Returns the safe payment details for a previously processed payment, or HTTP 404 when the identifier is unknown. The response contains only the last four card digits; the full card number and CVV are never returned.

### Example simulator outcomes

The supplied simulator decides the outcome from the final card-number digit:

- Odd digit (`1`, `3`, `5`, `7`, `9`): authorized.
- Even non-zero digit (`2`, `4`, `6`, `8`): declined.
- Zero: simulator returns HTTP 503; the gateway reports the outcome as unknown and does not save a payment as declined.

Example request (use a future expiry month/year):

```bash
curl -i -X POST http://localhost:8090/payment \
  -H 'Content-Type: application/json' \
  -d '{"card_number":"2222405343248877","expiry_month":12,"expiry_year":2099,"currency":"GBP","amount":100,"cvv":"123"}'
```

### Health

`GET /health` returns `{"status":"UP"}`.

## Design decisions

- **Layering:** the controller handles HTTP concerns, the service orchestrates payment processing, the bank client isolates the external HTTP integration, and the repository owns in-memory persistence.
- **Validation before side effects:** invalid requests are rejected before the bank is called.
- **Bank outcome mapping:** an HTTP 200 response with `authorized=true` maps to `Authorized`; `authorized=false` maps to `Declined`. Bank transport/server failures map to HTTP 503 because the payment outcome may be unknown. They are not treated as declines.
- **Sensitive data:** the full card number and CVV are sent to the simulator only for the request lifecycle. The repository and API response retain only the last four card digits. Request `toString()` redacts card data. Do not enable HTTP body logging for payment requests.
- **Thread safety:** the in-memory repository uses `ConcurrentHashMap` to support concurrent access. It is intentionally non-durable because the exercise explicitly permits a test-double repository.
- **Timeouts:** bank connection and read timeouts are configured to avoid indefinitely tying up request threads.
- **Observability:** every HTTP request receives or is assigned an `X-Correlation-Id`; it is returned in the response and included in the logging MDC. Payment logs include the generated payment ID and status, never card data.
- **Currency scope:** GBP, USD and EUR are supported as the exercise asks candidates to validate no more than three ISO currency codes.

## Testing

Run all automated tests:

```bash
./gradlew test
```

Tests cover payment validation (including card-number boundaries, expiry, currency and amount), authorization/decline mapping, bank invocation boundaries, bank-unavailable handling, safe response fields, bank payload serialization and payment retrieval.

The starter wrapper downloads Gradle 8.2.1 on first use if it is not already cached. If the build machine has no network access, run the same command on a machine with access to `services.gradle.org` and Maven Central.

## API documentation

With the application running, Swagger UI is available at `http://localhost:8090/swagger-ui/index.html`.

## Limitations and possible production improvements

- Payments are held in memory and are lost on restart; production would require durable, encrypted persistence and retention controls.
- Idempotency keys and duplicate-request handling would be required to safely retry payment requests. They are outside the explicit starter requirements and should be added before real-world use.
- A bank timeout can leave the outcome unknown. A production integration would need bank-side idempotency/query or reconciliation before retrying to avoid duplicate charges.
- Production card handling would require a PCI DSS-compliant card-data environment and tokenization; this exercise forwards card data only to its local simulator.
- Metrics, distributed tracing and alerting would be expanded using the organisation's standard observability platform.
