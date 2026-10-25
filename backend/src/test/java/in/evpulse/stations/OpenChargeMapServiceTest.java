package in.evpulse.stations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenChargeMapServiceTest {
    @Test
    void discoveryIsDisabledWhenApiKeyIsMissing() {
        OpenChargeMapService service = new OpenChargeMapService(RestClient.builder(), "");

        assertFalse(service.isConfigured());
        assertTrue(service.discoverHyderabad(null, null, null, null, 20).isEmpty());
    }

    @Test
    void unknownTariffsCannotPassMaximumPriceFilter() {
        OpenChargeMapService service = new OpenChargeMapService(RestClient.builder(), "configured-test-key");

        assertTrue(service.isConfigured());
        assertTrue(service.discoverHyderabad(
                null, null, null, new BigDecimal("20.00"), 20).isEmpty());
    }

    @Test
    void aMissingKeyIsTreatedAsUnconfigured() {
        OpenChargeMapService service = new OpenChargeMapService(RestClient.builder(), "  ");

        assertFalse(service.isConfigured());
    }
}
