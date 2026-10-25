# evPulse

EV charging discovery and tariff comparison, starting in Hyderabad, India.

evPulse helps drivers compare station locations, connector compatibility, charging power and published tariffs. It also aims to help charging operators maintain accurate station information.

## MVP principles

- Show the source and last-checked time for tariff information.
- Distinguish official/operator-confirmed tariffs from community reports and unverified records.
- Never imply live charger availability without an authorized, current data integration.
- Keep tariff validity, source provenance and freshness visible.
- Start in Hyderabad and expand only as data quality allows.

## Current implementation

- **Backend:** Java 21, Spring Boot, PostgreSQL and Flyway.
- **Frontend:** Next.js, React and TypeScript.
- **API:** `GET /api/v1/health`, `GET /api/v1/stations`.
- **Database:** operators, stations, connectors, tariff sources and tariffs.
- **CI:** backend verification and frontend production build.

## Local development

Requirements: Java 21+, Maven 3.9+, Node.js 22+, npm and PostgreSQL 16.

1. Start PostgreSQL with the root `docker-compose.yml`: `docker compose up -d db`.
2. Run the API: `cd backend && mvn spring-boot:run`.
3. In a second terminal, run the web app: `cd frontend && npm install && npm run dev`.
4. Open `http://localhost:3000`. The API is expected at `http://localhost:8080/api/v1`.

Database defaults are intended for local development only. Configure `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and `CORS_ALLOWED_ORIGINS` for your environment. The frontend API base can be overridden with `NEXT_PUBLIC_API_BASE_URL`.

## Station search

`GET /api/v1/stations?city=Hyderabad&q=Gachibowli&connectorType=CCS2&minPowerKw=50&maxPricePerKwh=20`

Filters are optional. Pagination uses `limit` (1–100) and `offset`. See [docs/api.md](docs/api.md).

## Data integrity

No guessed real-world station names or prices should be added. Each tariff must preserve its amount, billing model, validity window, fees where known, source, checked time and confidence. A tariff freshness flag is not a claim that a charger is available right now.
