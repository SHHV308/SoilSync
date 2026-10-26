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

## Free demo hosting and custom domain

The application includes a Dockerfile, supports the host-assigned `PORT`, and
has a Render Blueprint configured for the free web-service plan.

1. Sign in to [Render](https://render.com/). Deploy the public
   [`SHHV308/SoilSync` repository](https://github.com/SHHV308/SoilSync) as a
   Blueprint service. Review the plan and select **Free** before deploying.
2. Wait for the first Docker build and health check at `/api/health` to pass.
   The Render dashboard will show the public `https://…onrender.com` address.
3. In the web service's **Settings → Custom Domains**, add `soilsync.in`.
   Render will also add `www.soilsync.in`.
4. At your domain registrar or DNS provider, update the DNS records to the
   exact values shown by Render, then verify the domain in Render. Render
   provisions HTTPS automatically after DNS verification.

Free Render web services can go idle and take about a minute to start on the
next visit. This app's demo data uses an in-memory database and resets after a
restart or redeploy; don't use it for important farm records.

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
