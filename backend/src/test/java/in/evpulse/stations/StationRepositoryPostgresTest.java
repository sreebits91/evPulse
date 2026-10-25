package in.evpulse.stations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class StationRepositoryPostgresTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StationRepository repository;

    @BeforeEach
    void seedStationsWithFreshAndStaleTariffs() {
        jdbc.update("DELETE FROM tariffs");
        jdbc.update("DELETE FROM connectors");
        jdbc.update("DELETE FROM stations");

        long staleStationId = insertStation("Stale cheap tariff station");
        long freshStationId = insertStation("Fresh tariff station");
        long staleConnectorId = insertConnector(staleStationId);
        long freshConnectorId = insertConnector(freshStationId);

        jdbc.update("""
                INSERT INTO tariffs (station_id, connector_id, price_per_kwh, checked_at)
                VALUES (?, ?, 5.00, NOW() - INTERVAL '60 days')
                """, staleStationId, staleConnectorId);
        jdbc.update("""
                INSERT INTO tariffs (station_id, connector_id, price_per_kwh, checked_at)
                VALUES (?, ?, 10.00, NOW() - INTERVAL '1 day')
                """, freshStationId, freshConnectorId);
    }

    @Test
    void maximumPriceSearchExcludesStaleTariffsAndCountMatchesSearch() {
        List<StationOffer> offers = repository.search(
                "Hyderabad", null, null, null, new BigDecimal("20.00"), 20, 0);
        long count = repository.count(
                "Hyderabad", null, null, null, new BigDecimal("20.00"));

        assertEquals(1, offers.size());
        assertEquals("Fresh tariff station", offers.get(0).stationName());
        assertEquals(1L, count);
    }

    @Test
    void searchWithoutPriceCeilingCanStillShowStaleTariffWithFreshnessFlag() {
        List<StationOffer> offers = repository.search(
                "Hyderabad", null, null, null, null, 20, 0);

        assertEquals(2, offers.size());
        StationOffer staleOffer = offers.stream()
                .filter(offer -> offer.stationName().equals("Stale cheap tariff station"))
                .findFirst()
                .orElseThrow();
        assertEquals(false, staleOffer.tariffCurrent());
    }

    private long insertStation(String name) {
        return jdbc.queryForObject("""
                INSERT INTO stations
                    (name, address, locality, city, state, latitude, longitude, status)
                VALUES (?, '1 Test Road', 'Gachibowli', 'Hyderabad', 'Telangana',
                        17.400000, 78.400000, 'OPERATIONAL')
                RETURNING id
                """, Long.class, name);
    }

    private long insertConnector(long stationId) {
        return jdbc.queryForObject("""
                INSERT INTO connectors (station_id, connector_type, power_kw, quantity, current_type)
                VALUES (?, 'CCS2', 150.00, 2, 'DC')
                RETURNING id
                """, Long.class, stationId);
    }
}
