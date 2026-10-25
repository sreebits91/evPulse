package in.evpulse.stations;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StationRepository {
    private final JdbcTemplate jdbc;

    public StationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<StationOffer> MAPPER = (rs, rowNum) -> new StationOffer(
            rs.getLong("station_id"),
            rs.getString("station_name"),
            rs.getString("operator_name"),
            rs.getString("address"),
            rs.getString("locality"),
            rs.getString("city"),
            rs.getString("state"),
            rs.getBigDecimal("latitude"),
            rs.getBigDecimal("longitude"),
            rs.getString("station_status"),
            rs.getArray("amenities") == null ? List.of()
                    : List.of((String[]) rs.getArray("amenities").getArray()),
            rs.getString("connector_type"),
            rs.getBigDecimal("power_kw"),
            rs.getString("current_type"),
            rs.getInt("connector_quantity"),
            rs.getBigDecimal("price_per_kwh"),
            rs.getBigDecimal("session_fee"),
            rs.getString("currency"),
            rs.getString("pricing_model"),
            rs.getString("confidence"),
            rs.getString("source_name"),
            rs.getString("source_url"),
            rs.getObject("tariff_checked_at", java.time.OffsetDateTime.class),
            rs.getBoolean("tariff_current")
    );

    public List<StationOffer> search(
            String city, String query, String connectorType,
            BigDecimal minPowerKw, BigDecimal maxPricePerKwh,
            int limit, int offset) {
        String sql = """
            SELECT s.id AS station_id, s.name AS station_name, o.name AS operator_name,
                   s.address, s.locality, s.city, s.state, s.latitude, s.longitude,
                   s.status AS station_status, s.amenities,
                   c.connector_type, c.power_kw, c.current_type,
                   c.quantity AS connector_quantity,
                   t.price_per_kwh, t.session_fee, t.currency, t.pricing_model,
                   COALESCE(t.confidence, 'UNVERIFIED') AS confidence,
                   src.source_name, src.source_url,
                   t.checked_at AS tariff_checked_at,
                   (t.id IS NOT NULL AND t.valid_from <= NOW()
                     AND (t.valid_until IS NULL OR t.valid_until > NOW())
                     AND t.checked_at >= NOW() - INTERVAL '30 days') AS tariff_current
              FROM stations s
              LEFT JOIN operators o ON o.id = s.operator_id
              LEFT JOIN connectors c ON c.station_id = s.id
              LEFT JOIN LATERAL (
                  SELECT t1.* FROM tariffs t1
                   WHERE t1.station_id = s.id
                     AND (t1.connector_id IS NULL OR t1.connector_id = c.id)
                     AND t1.valid_from <= NOW()
                     AND (t1.valid_until IS NULL OR t1.valid_until > NOW())
                   ORDER BY CASE WHEN t1.connector_id = c.id THEN 1 ELSE 0 END DESC, t1.checked_at DESC
                   LIMIT 1
              ) t ON TRUE
              LEFT JOIN tariff_sources src ON src.id = t.source_id
             WHERE LOWER(s.city) = LOWER(?)
               AND (CAST(? AS text) IS NULL OR s.name ILIKE '%' || ? || '%'
                    OR COALESCE(s.locality, '') ILIKE '%' || ? || '%'
                    OR s.address ILIKE '%' || ? || '%'
                    OR COALESCE(o.name, '') ILIKE '%' || ? || '%')
               AND (CAST(? AS text) IS NULL OR c.connector_type = ?)
               AND (CAST(? AS numeric) IS NULL OR c.power_kw >= ?)
               AND (CAST(? AS numeric) IS NULL OR t.price_per_kwh <= ?)
             ORDER BY CASE WHEN t.price_per_kwh IS NULL THEN 1 ELSE 0 END,
                      t.price_per_kwh ASC, s.name ASC
             LIMIT ? OFFSET ?
            """;
        String q = blankToNull(query);
        String connector = blankToNull(connectorType);
        return jdbc.query(sql, MAPPER,
                city, q, q, q, q, q, q, connector, connector,
                minPowerKw, minPowerKw, maxPricePerKwh, maxPricePerKwh,
                limit, offset);
    }

    public long count(String city, String query, String connectorType,
                      BigDecimal minPowerKw, BigDecimal maxPricePerKwh) {
        String sql = """
            SELECT COUNT(DISTINCT s.id)
              FROM stations s
              LEFT JOIN operators o ON o.id = s.operator_id
              LEFT JOIN connectors c ON c.station_id = s.id
              LEFT JOIN LATERAL (
                  SELECT t1.* FROM tariffs t1
                   WHERE t1.station_id = s.id
                     AND (t1.connector_id IS NULL OR t1.connector_id = c.id)
                     AND t1.valid_from <= NOW()
                     AND (t1.valid_until IS NULL OR t1.valid_until > NOW())
                   ORDER BY (t1.connector_id = c.id) DESC, t1.checked_at DESC
                   LIMIT 1
              ) t ON TRUE
             WHERE LOWER(s.city) = LOWER(?)
               AND (? IS NULL OR s.name ILIKE '%' || ? || '%'
                    OR COALESCE(s.locality, '') ILIKE '%' || ? || '%'
                    OR s.address ILIKE '%' || ? || '%'
                    OR COALESCE(o.name, '') ILIKE '%' || ? || '%')
               AND (? IS NULL OR c.connector_type = ?)
               AND (? IS NULL OR c.power_kw >= ?)
               AND (? IS NULL OR t.price_per_kwh <= ?)
            """;
        String q = blankToNull(query);
        String connector = blankToNull(connectorType);
        Long result = jdbc.queryForObject(sql, Long.class,
                city, q, q, q, q, q, connector, connector,
                minPowerKw, minPowerKw, maxPricePerKwh, maxPricePerKwh);
        return result == null ? 0 : result;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}