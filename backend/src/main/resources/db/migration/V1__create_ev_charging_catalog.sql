CREATE TABLE operators (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL UNIQUE,
    website_url TEXT,
    support_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE stations (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT REFERENCES operators(id),
    name VARCHAR(200) NOT NULL,
    address TEXT NOT NULL,
    locality VARCHAR(120),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL DEFAULT 'Telangana',
    postal_code VARCHAR(20),
    latitude NUMERIC(9,6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(9,6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    access_type VARCHAR(30) NOT NULL DEFAULT 'PUBLIC'
        CHECK (access_type IN ('PUBLIC', 'SEMI_PUBLIC', 'PRIVATE')),
    status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN'
        CHECK (status IN ('OPERATIONAL', 'TEMPORARILY_CLOSED', 'UNKNOWN')),
    opening_hours TEXT,
    amenities TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE connectors (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    connector_type VARCHAR(40) NOT NULL,
    power_kw NUMERIC(7,2) NOT NULL CHECK (power_kw > 0),
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    current_type VARCHAR(10) NOT NULL CHECK (current_type IN ('AC', 'DC')),
    UNIQUE (station_id, connector_type, power_kw, current_type)
);

CREATE TABLE tariff_sources (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT REFERENCES operators(id),
    source_name VARCHAR(200) NOT NULL,
    source_url TEXT,
    source_type VARCHAR(30) NOT NULL
        CHECK (source_type IN ('OPERATOR', 'GOVERNMENT', 'PARTNER_API', 'COMMUNITY', 'MANUAL')),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tariffs (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    connector_id BIGINT REFERENCES connectors(id) ON DELETE SET NULL,
    source_id BIGINT REFERENCES tariff_sources(id),
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    price_per_kwh NUMERIC(10,2) CHECK (price_per_kwh >= 0),
    session_fee NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (session_fee >= 0),
    parking_fee_per_hour NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (parking_fee_per_hour >= 0),
    pricing_model VARCHAR(30) NOT NULL DEFAULT 'PER_KWH'
        CHECK (pricing_model IN ('PER_KWH', 'PER_MINUTE', 'FLAT', 'MIXED', 'UNKNOWN')),
    valid_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_until TIMESTAMPTZ,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confidence VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED'
        CHECK (confidence IN ('OFFICIAL', 'PARTNER_CONFIRMED', 'COMMUNITY_REPORTED', 'UNVERIFIED')),
    notes TEXT,
    CHECK (valid_until IS NULL OR valid_until > valid_from)
);

CREATE INDEX idx_stations_city_locality ON stations (LOWER(city), LOWER(locality));
CREATE INDEX idx_stations_geo ON stations (latitude, longitude);
CREATE INDEX idx_connectors_station_type ON connectors (station_id, connector_type);
CREATE INDEX idx_tariffs_station_validity ON tariffs (station_id, valid_from DESC, valid_until);
CREATE INDEX idx_tariffs_price ON tariffs (price_per_kwh) WHERE price_per_kwh IS NOT NULL;