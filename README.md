# evPulse

EV charging discovery and price comparison, starting in Hyderabad, India.

evPulse helps EV drivers compare charging locations, connectors, charging speeds, and published tariffs across operators. It also aims to help charging operators maintain accurate station information.

## MVP principles

- Show the source and last-verified time for tariff information.
- Distinguish published tariffs, community reports, and live operator telemetry.
- Never imply live availability unless an authorized, current data integration supports it.
- Normalize prices carefully; session cost estimates must state their assumptions.
- Start with Hyderabad and expand only as data quality allows.

## Initial architecture

- backend/: Java 21, Spring Boot, PostgreSQL/PostGIS.
- frontend/: planned TypeScript/Next.js driver experience.
- docs/: product, data, API, and operational decisions.

The backend starts as a modular monolith. Keep station data, connector details, tariffs, source provenance, and verification history separate so the model can evolve safely.

## Local development

Requirements: Java 21+, Maven 3.9+, and PostgreSQL 16 with PostGIS for database-backed development.

Run the API from the backend directory with: mvn spring-boot:run

The initial health endpoint is GET /api/v1/health.

Database configuration is read from environment variables. See backend/src/main/resources/application.yml.

## Data integrity

Do not add guessed station prices or present sample records as verified real-world data. Each tariff should eventually record its amount, billing unit, applicable time window, fees/taxes where known, source, observed time, and verification status.
