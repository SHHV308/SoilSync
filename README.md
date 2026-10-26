# SoilSync

SoilSync is a demo full-stack agri-tech web application designed for farmers, growers, and gardeners. The project combines a responsive website, a plant-care assistant, product catalog, reminders, and operational analytics in one Spring Boot application.

## Architecture

- Frontend: responsive static web app served by Spring Boot
- Backend: Java 17 + Spring Boot 3 + Spring MVC + Spring Data JPA
- Database: H2 in-memory database for local demo usage
- Regional crop advisor: live location lookup and weather forecast combined with crop-, growth-stage-, and soil-specific agronomy guidance

## Features

- Overview dashboard with key metrics
- Regional crop advisor using live weather for fertilizer timing and crop-stage guidance
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

Start the application:

```bash
mvn spring-boot:run
```

Then open:

- http://localhost:8080/
- http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:soilsync)

## Deploy a public demo on Railway

The application includes a Dockerfile and supports Railway's assigned `PORT`.

1. Sign in at [Railway](https://railway.com/) and create a project from the
   GitHub repository [`SHHV308/SoilSync`](https://github.com/SHHV308/SoilSync).
2. Deploy the service using the Dockerfile in the repository root.
3. In the service's **Settings → Networking**, choose **Generate Domain**.
   Share the generated `https://…up.railway.app` URL so anyone can open the
   app in their browser.
4. Optionally set the service health-check path to `/api/health`.

The demo uses an in-memory H2 database. Its sample data resets when the
application restarts or redeploys; use a persistent database before relying on
it for real farm records. Railway account setup and deployment require signing
in to Railway.

## Login

The demo app allows anonymous browsing. The UI does not require an account for the local demonstration.

## Regional crop advice

The advisor asks for an Indian town/district, crop, growth stage, and soil type. It geocodes the location and retrieves current conditions plus a three-day forecast from the public Open-Meteo APIs. It combines that live weather with crop-stage guidance and the selected soil type, and warns when rain or heat makes fertilizer application risky.

Weather and location lookups require an internet connection. If the live source is unavailable or the location cannot be found, the advisor shows an error rather than substituting generic or stale weather. Fertilizer rates are not guessed: exact quantities require a current soil test and the relevant district agriculture department or university crop package. The guidance is decision support, not a substitute for local agronomic advice.

## Notes

This is a professional demo project, not a production-grade agricultural certification or scientific guarantee system. Product claims and recommended application rates are presented as configurable demo content rather than verified agronomic guarantees.
