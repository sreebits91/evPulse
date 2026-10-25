"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";

type StationOffer = {
  stationId: number;
  stationName: string;
  operatorName: string | null;
  address: string;
  locality: string | null;
  city: string;
  state: string;
  latitude: number;
  longitude: number;
  stationStatus: string;
  amenities: string[];
  connectorType: string | null;
  powerKw: number | null;
  currentType: string | null;
  connectorQuantity: number;
  pricePerKwh: number | null;
  sessionFee: number | null;
  currency: string | null;
  pricingModel: string | null;
  confidence: string;
  sourceName: string | null;
  sourceUrl: string | null;
  tariffCheckedAt: string | null;
  tariffCurrent: boolean;
};

type SearchResponse = {
  city: string;
  total: number;
  limit: number;
  offset: number;
  offers: StationOffer[];
  dataQualityNote: string;
};

const API_BASE = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1").replace(/\/$/, "");

function money(value: number | null, currency: string | null) {
  if (value === null || value === undefined) return "Price not published";
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: currency || "INR",
    maximumFractionDigits: 2,
  }).format(value);
}

function formatCheckedAt(value: string | null) {
  if (!value) return "No check recorded";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Check date unavailable" : date.toLocaleDateString("en-IN", {
    day: "numeric", month: "short", year: "numeric",
  });
}

export default function Home() {
  const [city, setCity] = useState("Hyderabad");
  const [query, setQuery] = useState("");
  const [connector, setConnector] = useState("");
  const [minPower, setMinPower] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [data, setData] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [searchKey, setSearchKey] = useState(0);

  const search = useCallback(async () => {
    setLoading(true);
    setError("");
    const params = new URLSearchParams({ city: city.trim() || "Hyderabad", limit: "50" });
    if (query.trim()) params.set("q", query.trim());
    if (connector) params.set("connectorType", connector);
    if (minPower) params.set("minPowerKw", minPower);
    if (maxPrice) params.set("maxPricePerKwh", maxPrice);
    try {
      const response = await fetch(`${API_BASE}/stations?${params.toString()}`, { cache: "no-store" });
      if (!response.ok) throw new Error(`Station service returned HTTP ${response.status}`);
      setData(await response.json() as SearchResponse);
    } catch {
      setData(null);
      setError("We couldn’t reach the station service. Start the backend on port 8080 and try again.");
    } finally {
      setLoading(false);
    }
  }, [city, query, connector, minPower, maxPrice]);

  useEffect(() => { void search(); }, [search, searchKey]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSearchKey((value) => value + 1);
    void search();
  }

  return (
    <main>
      <nav className="nav wrap">
        <a className="brand" href="#" aria-label="evPulse home"><span className="brand-mark">e</span>vPulse<span className="brand-dot">.</span></a>
        <span className="nav-note"><span className="live-dot" /> Built for India’s EV journeys</span>
      </nav>

      <section className="hero">
        <div className="wrap hero-inner">
          <p className="eyebrow">CHARGE SMARTER</p>
          <h1>Find your charge.<br /><span>Know your price.</span></h1>
          <p className="hero-copy">Compare charging options before you plug in. Clear tariffs, connector details and source transparency — all in one place.</p>
          <div className="hero-pills"><span>↗ Price comparison</span><span>⚡ Connector fit</span><span>◎ Source-aware data</span></div>
        </div>
        <div className="hero-orbit orbit-one" /><div className="hero-orbit orbit-two" />
      </section>

      <section className="wrap search-section" aria-label="Search charging stations">
        <form className="search-panel" onSubmit={submit}>
          <div className="search-heading"><div><p className="eyebrow">YOUR NEXT STOP</p><h2>Explore charging stations</h2></div><span className="pilot-tag">HYDERABAD PILOT</span></div>
          <div className="filters">
            <label className="field city-field"><span>City</span><input value={city} onChange={(e) => setCity(e.target.value)} placeholder="e.g. Hyderabad" /></label>
            <label className="field query-field"><span>Station or area</span><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Try Gachibowli, Madhapur..." /></label>
            <label className="field"><span>Connector</span><select value={connector} onChange={(e) => setConnector(e.target.value)}><option value="">Any connector</option><option value="CCS2">CCS2</option><option value="TYPE2">Type 2</option><option value="CHADEMO">CHAdeMO</option><option value="GB/T">GB/T</option><option value="Bharat AC-001">Bharat AC-001</option><option value="Bharat DC-001">Bharat DC-001</option></select></label>
            <label className="field compact-field"><span>Min power (kW)</span><input inputMode="decimal" type="number" min="0.1" step="0.1" value={minPower} onChange={(e) => setMinPower(e.target.value)} placeholder="Any" /></label>
            <label className="field compact-field"><span>Max ₹ / kWh</span><input inputMode="decimal" type="number" min="0" step="0.5" value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} placeholder="Any" /></label>
            <button className="search-button" type="submit" disabled={loading}>{loading ? "Searching…" : "Find chargers"} <span>↗</span></button>
          </div>
        </form>
      </section>

      <section className="wrap results-section">
        <div className="results-title"><div><p className="eyebrow">CHARGING, MADE CLEAR</p><h2>{data ? `${data.total} matching offers` : "Your charging shortlist"}</h2></div><span className="results-city">⌖ {city || "Hyderabad"}</span></div>
        {error && <div className="notice error-notice"><strong>Service unavailable</strong><p>{error}</p></div>}
        {!error && loading && <div className="empty-state"><div className="loader" /><p>Finding matching charging options…</p></div>}
        {!error && !loading && data && data.offers.length === 0 && <div className="empty-state"><div className="empty-icon">⚡</div><h3>No verified station records yet</h3><p>There are no station records matching this search in the current catalog. We won’t invent station names or prices. Add sourced station data to the backend to populate this view.</p></div>}
        {!error && !loading && data && data.offers.length > 0 && <div className="station-grid">{data.offers.map((station, index) => (
          <article className="station-card" key={`${station.stationId}-${station.connectorType ?? "none"}-${index}`}>
            <div className="card-top"><span className="station-icon">ϟ</span><span className={station.tariffCurrent ? "status status-current" : "status status-stale"}>{station.tariffCurrent ? "Tariff recently checked" : "Check price first"}</span></div>
            <h3>{station.stationName}</h3><p className="station-location">{station.locality ? `${station.locality}, ` : ""}{station.city}</p>
            <p className="station-address">{station.address}</p>
            <div className="price-block"><span>Energy tariff</span><strong>{money(station.pricePerKwh, station.currency)}{station.pricePerKwh !== null && <small> / kWh</small>}</strong></div>
            <div className="specs"><div><span>CONNECTOR</span><strong>{station.connectorType ?? "Not listed"}</strong></div><div><span>MAX POWER</span><strong>{station.powerKw !== null ? `${station.powerKw} kW` : "Not listed"}</strong></div><div><span>PLUGS</span><strong>{station.connectorQuantity || "Not listed"}</strong></div></div>
            <div className="source-row"><span>Source: {station.sourceName ?? "Not recorded"}</span><span>Checked {formatCheckedAt(station.tariffCheckedAt)}</span></div>
            <div className="card-bottom"><span className={station.confidence === "OFFICIAL" || station.confidence === "PARTNER_CONFIRMED" ? "confidence confidence-strong" : "confidence"}>{station.confidence.replaceAll("_", " ").toLowerCase()}</span>{station.sourceUrl && <a href={station.sourceUrl} target="_blank" rel="noreferrer">View source ↗</a>}</div>
          </article>
        ))}</div>}
        <p className="data-note">ⓘ {data?.dataQualityNote ?? "Tariff freshness is not live charger availability. Confirm final prices and connector status with the operator before travelling."}</p>
      </section>

      <footer className="footer"><div className="wrap footer-inner"><a className="brand footer-brand" href="#"><span className="brand-mark">e</span>vPulse<span className="brand-dot">.</span></a><p>Better charging decisions start with better information.</p><span>Hyderabad first · India next</span></div></footer>
    </main>
  );
}