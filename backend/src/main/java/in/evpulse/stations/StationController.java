package in.evpulse.stations;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stations")
@Validated
public class StationController {
    private final StationRepository repository;
    private final OpenChargeMapService openChargeMapService;

    public StationController(StationRepository repository, OpenChargeMapService openChargeMapService) {
        this.repository = repository;
        this.openChargeMapService = openChargeMapService;
    }

    @GetMapping
    public StationSearchResponse search(
            @RequestParam(defaultValue = "Hyderabad") String city,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String connectorType,
            @RequestParam(required = false) @DecimalMin("0.1") BigDecimal minPowerKw,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal maxPricePerKwh,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        if (city.isBlank() || city.length() > 100) {
            throw new IllegalArgumentException("city must be between 1 and 100 characters");
        }

        String normalizedCity = city.trim();
        List<StationOffer> offers = repository.search(
                normalizedCity, q, connectorType, minPowerKw, maxPricePerKwh, limit, offset);
        long total = repository.count(normalizedCity, q, connectorType, minPowerKw, maxPricePerKwh);
        String dataQualityNote = "Prices may be missing or stale. Check tariffCheckedAt, confidence and source before relying on a tariff.";

        // The database remains the source of curated records. When it has no match,
        // optionally use Open Charge Map as a read-only discovery fallback for the pilot city.
        if (offers.isEmpty() && total == 0 && offset == 0
                && normalizedCity.equalsIgnoreCase("Hyderabad")) {
            List<StationOffer> discovered = openChargeMapService.discoverHyderabad(
                    q, connectorType, minPowerKw, maxPricePerKwh, limit);
            if (!discovered.isEmpty()) {
                offers = discovered;
                total = discovered.size();
                dataQualityNote = "Discovery records supplied by Open Charge Map (open data only). Tariffs are not verified or inferred; confirm details with the operator. This is not live charger availability.";
            } else if (!openChargeMapService.isConfigured()) {
                dataQualityNote += " To enable sourced station discovery, configure OPENCHARGEMAP_API_KEY. No sample station data is fabricated.";
            } else {
                dataQualityNote += " No matching records were returned by the configured discovery source.";
            }
        }
        return new StationSearchResponse(normalizedCity, total, limit, offset, offers, dataQualityNote);
    }

    public record StationSearchResponse(
            String city, long total, int limit, int offset,
            List<StationOffer> offers, String dataQualityNote) {}
}
