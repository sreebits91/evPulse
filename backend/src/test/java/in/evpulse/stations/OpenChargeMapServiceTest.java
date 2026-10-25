package in.evpulse.stations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenChargeMapServiceTest {
    @Test
    void stationStatusUsesExactOperationalMatch() {
        assertEquals("OPERATIONAL", OpenChargeMapService.normalizeStationStatus("Operational"));
        assertEquals("TEMPORARILY_CLOSED", OpenChargeMapService.normalizeStationStatus("Not Operational"));
        assertEquals("TEMPORARILY_CLOSED", OpenChargeMapService.normalizeStationStatus("Temporarily Unavailable"));
        assertEquals("UNKNOWN", OpenChargeMapService.normalizeStationStatus("Planned"));
        assertEquals("UNKNOWN", OpenChargeMapService.normalizeStationStatus(null));
    }

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
    void providerResponseMapsStatusAndCcsConnector() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("countrycode=IN")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{
                          "ID": 123,
                          "StatusType": {"Title": "Operational"},
                          "AddressInfo": {
                            "Title": "Test Charge Hub",
                            "AddressLine1": "1 Test Road",
                            "AddressLine2": "Gachibowli",
                            "Town": "Hyderabad",
                            "StateOrProvince": "Telangana",
                            "Latitude": 17.4,
                            "Longitude": 78.4
                          },
                          "Connections": [{
                            "ConnectionType": {"Title": "CCS (Type 2)"},
                            "PowerKW": 150,
                            "CurrentType": {"Title": "DC"},
                            "Quantity": 2
                          }]
                        }]
                        """, MediaType.APPLICATION_JSON));

        OpenChargeMapService service = new OpenChargeMapService(builder, "test-key");
        List<StationOffer> offers = service.discoverHyderabad(
                "Gachibowli", "CCS2", new BigDecimal("100"), null, 20);

        assertEquals(1, offers.size());
        assertEquals("OPERATIONAL", offers.get(0).stationStatus());
        assertEquals("CCS2", offers.get(0).connectorType());
        assertEquals(new BigDecimal("150"), offers.get(0).powerKw());
        server.verify();
    }

    @Test
    void aMissingKeyIsTreatedAsUnconfigured() {
        OpenChargeMapService service = new OpenChargeMapService(RestClient.builder(), "  ");

        assertFalse(service.isConfigured());
    }
}
