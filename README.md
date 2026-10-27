# SoilSync

SoilSync is a demo full-stack agri-tech web application designed for farmers, growers, and gardeners. The project combines a responsive website, a plant-care assistant, product catalog, reminders, and operational analytics in one Spring Boot application.

## Live website

Visit the deployed demo: **[https://soilsync.onrender.com](https://soilsync.onrender.com)**

The app is hosted on Render's free web-service plan. It may take about a minute
to wake up after a period of inactivity.

## Architecture

- Frontend: responsive static web app served by Spring Boot
- Backend: Java 17 + Spring Boot 3 + Spring MVC + Spring Data JPA
- Database: H2 in-memory for local development and PostgreSQL for deployment, managed by Flyway migrations
- Regional crop advisor: live location lookup and weather forecast combined with crop-, growth-stage-, and soil-specific agronomy guidance

## Features

- Overview dashboard with key metrics
- Worldwide place search and coordinate-based crop advisor with live weather and explicit coverage limits
- Plant journal and care tracking
- Reminder scheduler for watering, pruning, and fertilization
- Product catalog for organic soil products
- Order summary and management
- Business analytics panel for farm operations

## Running locally

Requirements: Java 17 and Maven.

Clone the repository:

```bash
git clone https://github.com/SHHV308/SoilSync.git
cd SoilSync
```

Start the application with the default `local` profile:

```bash
mvn spring-boot:run
```

Then open:

- http://localhost:8080/
- http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:soilsync)

Flyway creates and validates the schema at startup. The local H2 database is
in-memory and resets when the process stops.

## Hosting and custom domain

The live demo is deployed from this GitHub repository using the included
Render Blueprint and Dockerfile. If automatic deploys are enabled in Render,
pushes to `main` publish updates to the live website.

The Render Blueprint selects the `postgres` Spring profile and declares these
environment variables as secret/manual configuration:

- `SPRING_DATASOURCE_URL`: the JDBC URL from your PostgreSQL provider, for example
  `jdbc:postgresql://<host>:5432/<database>?sslmode=require`
- `SPRING_DATASOURCE_USERNAME`: database username
- `SPRING_DATASOURCE_PASSWORD`: database password

For the planned Neon setup, create a PostgreSQL database and copy its JDBC
connection details into the Render service's environment settings. Do not put
credentials in `render.yaml`, Dockerfile, or source control. Flyway applies
schema migrations on startup. Existing in-memory H2 data is not transferred to
PostgreSQL.

To connect the custom domain `soilsync.in`, add it in the Render web service's
**Settings → Custom Domains**. Then set the DNS records at your domain
registrar to the exact values Render provides and verify the domain in Render.
Render provisions HTTPS after DNS verification.

Free Render web services can go idle and take about a minute to start on the
next visit. Configure PostgreSQL before relying on records across restarts or
deploys.

## Login

The demo app allows anonymous browsing. The UI does not require an account for the local demonstration.

## Global location and crop-advice coverage

The advisor searches place names worldwide with Open-Meteo geocoding. If a
name matches multiple places, select the result with the correct country and
region. For coordinates, enter the latitude, longitude, country, two-letter
country code, and region yourself; coordinates are not reverse-geocoded.
Weather lookup uses the selected coordinates and Open-Meteo's automatic
timezone. The Köppen-Geiger climate zone is shown as `UNCLASSIFIED` until a
vetted classification dataset is selected and integrated.

The advisor uses only country/crop documents that have complete source
metadata and `status: vetted`. No regional documents are currently vetted.
Until a source is reviewed, answers explicitly state that local crop coverage
is missing and provide only general safety principles. It does not invent
regional recommendations or calculate fertilizer rates.

See [`src/main/resources/knowledge/README.md`](src/main/resources/knowledge/README.md)
for the document structure, required citations, review workflow, and
placeholders. Location and weather lookups require an internet connection. If
a live source is unavailable, the advisor reports an error instead of using
stale or generic weather. All advice is decision support, not a substitute
for local agronomic advice.

## Advisory logging and privacy

Advisory logging is off unless a browser explicitly opts in through
`POST /api/privacy/consent` with `{"accepted":true}`. The consent response sets
a random, host-only, HttpOnly cookie; the browser identifier is not an account
or a name/email. `GET /api/privacy/consent` reports consent status. To revoke
consent and delete that browser's advisory records, call
`DELETE /api/privacy/data` from the same browser. The delete endpoint also
expires the cookie.

With consent, SoilSync stores the selected country and administrative region
(not exact coordinates), crop, growth stage, soil type, weather snapshot, risk
flag, question and answer, sources, feedback, and application outcome. Common
email, phone, IPv4, street-address, and credential patterns are redacted from
free text before storage. This automated filtering is not comprehensive:
names and other personal details may not be detected, so do not submit
personal or sensitive information. The API returns an `advisoryLogId` only
when a record was stored. Feedback can be submitted to
`PUT /api/privacy/advisories/{id}/feedback` with `{"feedback":"HELPFUL"}` or
`{"feedback":"NOT_HELPFUL"}`. The application outcome endpoint is
`PUT /api/privacy/advisories/{id}/outcome` with `DEFERRED_APPLICATION`,
`APPLIED_ANYWAY`, or `UNKNOWN`.

The current interface does not yet include consent, feedback, or deletion
controls; these APIs are available for the planned frontend phase. Logging is
not enabled merely by visiting or using the advisor.

## Notes

This is a professional demo project, not a production-grade agricultural certification or scientific guarantee system. Product claims and recommended application rates are presented as configurable demo content rather than verified agronomic guarantees.
