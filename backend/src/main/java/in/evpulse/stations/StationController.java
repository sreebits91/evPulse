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

    public StationController(StationRepository repository) {
        this.repository = repository;
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
        List<StationOffer> offers = repository.search(
                city.trim(), q, connectorType, minPowerKw, maxPricePerKwh, limit, offset);
        long total = repository.count(city.trim(), q, connectorType, minPowerKw, maxPricePerKwh);
        return new StationSearchResponse(city.trim(), total, limit, offset, offers,
                "Prices may be missing or stale. Check tariffCheckedAt, confidence and source before relying on a tariff.");
    }

    public record StationSearchResponse(
            String city, long total, int limit, int offset,
            List<StationOffer> offers, String dataQualityNote) {}
}