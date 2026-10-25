package in.evpulse.stations;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record StationOffer(
        long stationId,
        String stationName,
        String operatorName,
        String address,
        String locality,
        String city,
        String state,
        BigDecimal latitude,
        BigDecimal longitude,
        String stationStatus,
        List<String> amenities,
        String connectorType,
        BigDecimal powerKw,
        String currentType,
        int connectorQuantity,
        BigDecimal pricePerKwh,
        BigDecimal sessionFee,
        String currency,
        String pricingModel,
        String confidence,
        String sourceName,
        String sourceUrl,
        OffsetDateTime tariffCheckedAt,
        boolean tariffCurrent
) {}