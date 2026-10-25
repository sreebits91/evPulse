package in.evpulse.stations;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Read-only discovery adapter for Open Charge Map. Tariffs are never inferred
 * from location or connector data; the provider commonly has no structured tariff.
 */
@Service
public class OpenChargeMapService {
    private static final String BASE_URL = "https://api.openchargemap.io/v3/poi/";
    private static final double HYDERABAD_LATITUDE = 17.3850;
    private static final double HYDERABAD_LONGITUDE = 78.4867;

    private final RestClient restClient;
    private final String apiKey;

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
        if (!isConfigured()) {
            return List.of();
        }
        // Unknown prices cannot honestly satisfy a maximum-price filter.
        if (maxPricePerKwh != null) {
            return List.of();
        }

        String uri = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("output", "json")
                .queryParam("countrycode", "IN")
                .queryParam("latitude", HYDERABAD_LATITUDE)
                .queryParam("longitude", HYDERABAD_LONGITUDE)
                .queryParam("distance", 30)
                .queryParam("distanceunit", "KM")
                .queryParam("maxresults", Math.min(Math.max(limit * 3, 30), 100))
                .queryParam("opendata", "true")
                .queryParam("compact", "false")
                .queryParam("verbose", "false")
                .build()
                .encode()
                .toUriString();

        try {
            JsonNode response = restClient.get()
                    .uri(uri)
                    .header("X-API-Key", apiKey)
                    .header(HttpHeaders.USER_AGENT, "evPulse/0.1 (EV charging discovery)")
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isArray()) {
                return List.of();
            }

            List<StationOffer> offers = new ArrayList<>();
            for (JsonNode poi : response) {
                JsonNode addressInfo = poi.path("AddressInfo");
                if (addressInfo.isMissingNode() || addressInfo.isNull()) {
                    continue;
                }
                String name = text(addressInfo, "Title");
                String address = text(addressInfo, "AddressLine1");
                String city = firstNonBlank(text(addressInfo, "Town"), "Hyderabad");
                if (name.isBlank() || address.isBlank()) {
                    continue;
                }
                String locality = text(addressInfo, "StateOrProvince");
                if (matchesQuery(query, name, address, locality, city) == false) {
                    continue;
                }

                JsonNode connections = poi.path("Connections");
                if (connections.isArray()) {
                    for (JsonNode connection : connections) {
                        JsonNode connectionType = connection.path("ConnectionType");
                        String type = firstNonBlank(text(connectionType, "Title"), "Not listed");
                        BigDecimal power = decimal(connection, "PowerKW");
                        String current = firstNonBlank(text(connection.path("CurrentType"), "Title"), "");
                        if (connectorType != null && !connectorType.isBlank()
                                && !type.equalsIgnoreCase(connectorType)) {
                            continue;
                        }
                        if (minPowerKw != null && (power == null || power.compareTo(minPowerKw) < 0)) {
                            continue;
                        }
                        offers.add(toOffer(poi, addressInfo, connection, name, address, locality,
                                city, type, power, current));
                    }
                } else {
                    offers.add(toOffer(poi, addressInfo, null, name, address, locality, city,
                            null, null, null));
                }
                if (offers.size() >= limit) {
                    break;
                }
            }
            return List.copyOf(offers.subList(0, Math.min(limit, offers.size())));
        } catch (RestClientException exception) {
            // Discovery is supplemental; a provider outage should not take down search.
            return List.of();
        }
    }

    private static StationOffer toOffer(
            JsonNode poi, JsonNode addressInfo, JsonNode connection,
            String name, String address, String locality, String city,
            String connectorType, BigDecimal powerKw, String currentType) {
        long id = poi.path("ID").asLong(0);
        String status = text(poi.path("StatusType"), "Title").toLowerCase();
        String stationStatus = status.contains("operational") ? "OPERATIONAL" : "UNKNOWN";
        String checked = text(poi, "DateLastVerified");
        OffsetDateTime checkedAt = null;
        if (!checked.isBlank()) {
            try {
                checkedAt = OffsetDateTime.parse(checked);
            } catch (Exception ignored) {
                // Invalid upstream dates are represented as unavailable.
            }
        }
        int quantity = connection == null ? 0 : connection.path("Quantity").asInt(0);
        if (quantity < 0) quantity = 0;
        BigDecimal latitude = decimal(addressInfo, "Latitude");
        BigDecimal longitude = decimal(addressInfo, "Longitude");
        String operator = text(poi.path("OperatorInfo"), "Title");
        return new StationOffer(
                -Math.max(id, 1), name, operator.isBlank() ? null : operator, address,
                locality.isBlank() ? null : locality, city,
                firstNonBlank(text(addressInfo, "StateOrProvince"), "Telangana"),
                latitude, longitude, stationStatus, List.of(),
                connectorType, powerKw, currentType.isBlank() ? null : currentType,
                quantity, null, null, "INR", null, "UNVERIFIED",
                "Open Charge Map", "https://openchargemap.org/site/poi/details/" + id,
                checkedAt, false);
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
