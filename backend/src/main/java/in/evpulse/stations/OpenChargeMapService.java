package in.evpulse.stations;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Read-only discovery adapter for Open Charge Map. Upstream results are cached
 * briefly to avoid repeated identical API calls. Tariffs are never inferred.
 */
@Service
public class OpenChargeMapService {
    private static final String BASE_URL = "https://api.openchargemap.io/v3/poi/";
    private static final double HYDERABAD_LATITUDE = 17.3850;
    private static final double HYDERABAD_LONGITUDE = 78.4867;
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;

    private final RestClient restClient;
    private final String apiKey;
    private volatile List<JsonNode> cachedPois = List.of();
    private volatile long cacheExpiresAtMillis;

    public OpenChargeMapService(
            RestClient.Builder builder,
            @Value("${evpulse.openchargemap.api-key:}") String apiKey) {
        this.restClient = builder.baseUrl(BASE_URL).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public List<StationOffer> discoverHyderabad(
            String query, String connectorType, BigDecimal minPowerKw,
            BigDecimal maxPricePerKwh, int limit) {
        if (!isConfigured() || maxPricePerKwh != null) {
            // Unknown prices cannot honestly satisfy a maximum-price filter.
            return List.of();
        }

        List<StationOffer> offers = new ArrayList<>();
        for (JsonNode poi : fetchPois()) {
            JsonNode addressInfo = poi.path("AddressInfo");
            if (addressInfo.isMissingNode() || addressInfo.isNull()) {
                continue;
            }
            String name = text(addressInfo, "Title");
            String address = text(addressInfo, "AddressLine1");
            String city = firstNonBlank(text(addressInfo, "Town"), "Hyderabad");
            if (name.isBlank() || address.isBlank()
                    || !matchesQuery(query, name, address, text(addressInfo, "AddressLine2"), city)) {
                continue;
            }

            JsonNode connections = poi.path("Connections");
            if (connections.isArray()) {
                for (JsonNode connection : connections) {
                    String type = normalizeConnector(text(connection.path("ConnectionType"), "Title"));
                    BigDecimal power = decimal(connection, "PowerKW");
                    String current = firstNonBlank(text(connection.path("CurrentType"), "Title"), "");
                    if (connectorType != null && !connectorType.isBlank()
                            && !type.equalsIgnoreCase(normalizeConnector(connectorType))) {
                        continue;
                    }
                    if (minPowerKw != null && (power == null || power.compareTo(minPowerKw) < 0)) {
                        continue;
                    }
                    offers.add(toOffer(poi, addressInfo, connection, name, address,
                            text(addressInfo, "AddressLine2"), city, type, power, current));
                    if (offers.size() >= limit) break;
                }
            } else if (connectorType == null && minPowerKw == null) {
                offers.add(toOffer(poi, addressInfo, null, name, address,
                        text(addressInfo, "AddressLine2"), city, null, null, null));
            }
            if (offers.size() >= limit) break;
        }
        return List.copyOf(offers.subList(0, Math.min(limit, offers.size())));
    }

    private List<JsonNode> fetchPois() {
        long now = System.currentTimeMillis();
        if (now < cacheExpiresAtMillis) return cachedPois;
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now < cacheExpiresAtMillis) return cachedPois;
            String uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("output", "json")
                    .queryParam("countrycode", "IN")
                    .queryParam("latitude", HYDERABAD_LATITUDE)
                    .queryParam("longitude", HYDERABAD_LONGITUDE)
                    .queryParam("distance", 30)
                    .queryParam("distanceunit", "KM")
                    .queryParam("maxresults", 100)
                    .queryParam("opendata", "true")
                    .queryParam("compact", "false")
                    .queryParam("verbose", "false")
                    .build().encode().toUriString();
            try {
                JsonNode response = restClient.get()
                        .uri(uri)
                        .header("X-API-Key", apiKey)
                        .header(HttpHeaders.USER_AGENT, "evPulse/0.1 (EV charging discovery)")
                        .retrieve()
                        .body(JsonNode.class);
                List<JsonNode> records = new ArrayList<>();
                if (response != null && response.isArray()) {
                    response.forEach(records::add);
                }
                cachedPois = List.copyOf(records);
            } catch (RestClientException exception) {
                // Cache the empty response briefly; provider failure never breaks curated search.
                cachedPois = List.of();
            }
            cacheExpiresAtMillis = System.currentTimeMillis() + CACHE_TTL_MILLIS;
            return cachedPois;
        }
    }

    private static StationOffer toOffer(
            JsonNode poi, JsonNode addressInfo, JsonNode connection,
            String name, String address, String locality, String city,
            String connectorType, BigDecimal powerKw, String currentType) {
        long id = poi.path("ID").asLong(0);
        String stationStatus = normalizeStationStatus(text(poi.path("StatusType"), "Title"));
        int quantity = connection == null ? 0 : connection.path("Quantity").asInt(0);
        if (quantity < 0) quantity = 0;
        String operator = text(poi.path("OperatorInfo"), "Title");
        String attribution = providerAttribution(poi);
        return new StationOffer(
                -Math.max(id, 1), name, operator.isBlank() ? null : operator, address,
                locality.isBlank() ? null : locality, city,
                firstNonBlank(text(addressInfo, "StateOrProvince"), "Telangana"),
                decimal(addressInfo, "Latitude"), decimal(addressInfo, "Longitude"),
                stationStatus, List.of(), connectorType, powerKw,
                currentType.isBlank() ? null : currentType, quantity,
                null, null, "INR", null, "UNVERIFIED",
                attribution, "https://openchargemap.org/site/poi/details/" + id,
                null, false);
    }

    /**
     * Open Charge Map requires visible attribution for the data provider and
     * applicable license. Preserve this information in the source label shown
     * on each station card instead of attributing every record to OCM alone.
     */
    private static String providerAttribution(JsonNode poi) {
        JsonNode providers = poi.path("DataProviders");
        if (providers.isArray()) {
            for (JsonNode entry : providers) {
                JsonNode provider = entry.path("DataProvider");
                String title = text(provider, "Title");
                String license = text(entry, "License");
                boolean openData = entry.path("IsOpenData").asBoolean(false);
                if (!title.isBlank()) {
                    StringBuilder attribution = new StringBuilder("Open Charge Map · ").append(title);
                    if (!license.isBlank()) attribution.append(" · ").append(license);
                    if (openData) attribution.append(" · Open Data");
                    return attribution.toString();
                }
            }
        }
        return "Open Charge Map · Open Data (provider attribution unavailable; open source record)";
    }

    static String normalizeStationStatus(String status) {
        if (status == null || status.isBlank()) return "UNKNOWN";
        String normalized = status.trim().toLowerCase();
        if (normalized.equals("operational")) return "OPERATIONAL";
        if (normalized.equals("not operational")
                || normalized.contains("temporarily unavailable")
                || normalized.contains("temporarily closed")
                || normalized.equals("decommissioned")) {
            return "TEMPORARILY_CLOSED";
        }
        return "UNKNOWN";
    }

    private static String normalizeConnector(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (normalized.contains("chademo")) return "CHADEMO";
        if (normalized.contains("ccs") || normalized.contains("combo2")) return "CCS2";
        if (normalized.contains("type2") || normalized.contains("iec62196")) return "TYPE2";
        if (normalized.contains("gbt")) return "GB/T";
        return value.trim().toUpperCase();
    }

    private static boolean matchesQuery(String query, String... fields) {
        if (query == null || query.isBlank()) return true;
        String needle = query.trim().toLowerCase();
        for (String field : fields) {
            if (field != null && field.toLowerCase().contains(needle)) return true;
        }
        return false;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        return value == null || value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        return value == null || value.isMissingNode() || value.isNull() || !value.isNumber()
                ? null : value.decimalValue();
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
