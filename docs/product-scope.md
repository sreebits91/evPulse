# evPulse MVP: Hyderabad

## Driver outcomes

A driver should be able to search a Hyderabad area, filter stations by connector and charging power, compare tariff structures, estimate a session cost, and open the operator's official destination or navigation app.

## Operator outcomes

An operator should eventually be able to claim stations, submit tariff and connector updates, and see referrals and data-quality issues. Operator claims must be reviewed before they become verified public data.

## Station coverage approach

Start with a controlled inventory for Gachibowli and the Financial District, Madhapur, and the Shamshabad/airport corridor. These are proposed pilot areas, not a claim that coverage is already available.

Use operator-published station information, authorized feeds, permitted public datasets, and clearly labelled community reports. Respect source terms and licensing. Do not scrape restricted systems or imply a partnership without written agreement.

## Tariff data requirements

A tariff record should support:
- currency and amount;
- billing basis, such as per kWh, per minute, per session, or a combination;
- applicable time window and conditions;
- taxes, parking, idle, and other fees when published;
- source URL or source identifier;
- observed-at and last-verified timestamps;
- verification state and verifier;
- validity period and history.

Do not compare unlike billing units as if they were identical. If the full cost is unknown, label the estimate as partial and explain what is excluded.

## Availability and reliability

Show live availability only when it comes from a current, authorized operator integration. User reports are reports, not live telemetry. Display the source and timestamp for each status and let users flag stale or incorrect information.

## Out of scope for the first MVP

- starting or stopping a charge session;
- storing a unified wallet or payment credentials;
- guaranteed reservations;
- nationwide real-time charger telemetry;
- predictive availability before reliable history exists.

## Initial success measures

These are targets to test, not current results:
- a deduplicated Hyderabad station inventory with provenance for every record;
- tariff freshness measured against a defined operator-specific policy;
- successful compatible-station searches;
- repeat use by drivers;
- pilot feedback and evidence of operator willingness to participate.
