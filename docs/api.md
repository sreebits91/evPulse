# API v1

Base path: `/api/v1`

## Health

`GET /api/v1/health`

## Search stations and compare tariffs

`GET /api/v1/stations`

Query parameters:

| Parameter | Default | Meaning |
| --- | --- | --- |
| `city` | `Hyderabad` | Case-insensitive city filter |
| `q` | none | Search station name, locality, address or operator |
| `connectorType` | none | Exact connector type, e.g. CCS2 or TYPE2 |
| `minPowerKw` | none | Minimum connector power |
| `maxPricePerKwh` | none | Maximum tariff in the tariff currency |
| `limit` | 20 | Page size, 1–100 |
| `offset` | 0 | Number of result rows to skip |

Example:

```http
GET /api/v1/stations?city=Hyderabad&connectorType=CCS2&minPowerKw=50&limit=20
```

The response includes station details, connector details, tariff, source and last-check time. Missing tariffs are allowed and appear as null. A tariff is marked current only when it is within its validity window and was checked within the last 30 days. This is a freshness heuristic, not proof that a charger is currently available.

## Open Charge Map discovery fallback

When the curated database has no matching results for Hyderabad, evPulse can optionally query Open Charge Map for openly licensed location records. Configure `OPENCHARGEMAP_API_KEY` in the environment (register a key through [Open Charge Map](https://www.openchargemap.org/develop/api)). The key is sent server-side in the `X-API-Key` header and is never exposed to the browser.

Discovery is limited to a 30 km radius around central Hyderabad and at most 100 upstream records per request. The adapter identifies the source and links to the provider record. Discovery records are not automatically written into the curated database. Tariff values remain unknown unless independently sourced; a maximum-price filter excludes records with unknown prices. Provider failures return no discovery records rather than breaking the curated search.

Open Charge Map is an independent source; evPulse does not imply endorsement or partnership. Its API is subject to provider terms and licensing. Only records marked as open data are requested. See the [Open Charge Map API documentation](https://www.openchargemap.org/develop/api).

## Data quality

- Do not interpret `stationStatus` as live charger availability.
- `confidence` identifies the tariff evidence class.
- `sourceName` and `sourceUrl` preserve provenance.
- Do not add production station prices until there is a traceable, permitted source.
- API responses do not imply a partnership with any charging operator.
