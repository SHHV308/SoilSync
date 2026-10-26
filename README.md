# SoilSync

SoilSync is a demo full-stack agri-tech web application designed for farmers, growers, and gardeners. The project combines a responsive website, a plant-care assistant, product catalog, reminders, and operational analytics in one Spring Boot application.

## Live website

Visit the deployed demo: **[https://soilsync.onrender.com](https://soilsync.onrender.com)**

The app is hosted on Render's free web-service plan. It may take about a minute
to wake up after a period of inactivity.

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

## Hosting and custom domain

The live demo is deployed from this GitHub repository using the included
Render Blueprint and Dockerfile. If automatic deploys are enabled in Render,
pushes to `main` publish updates to the live website.

To connect the custom domain `soilsync.in`, add it in the Render web service's
**Settings → Custom Domains**. Then set the DNS records at your domain
registrar to the exact values Render provides and verify the domain in Render.
Render provisions HTTPS after DNS verification.

Free Render web services can go idle and take about a minute to start on the
next visit. This app's demo data uses an in-memory H2 database and resets after a restart
or redeploy; don't use it for important farm records.

## Login

The demo app allows anonymous browsing. The UI does not require an account for the local demonstration.

## Regional crop advice

The advisor asks for an Indian town/district, crop, growth stage, and soil type. It geocodes the location and retrieves current conditions plus a three-day forecast from the public Open-Meteo APIs. It combines that live weather with crop-stage guidance and the selected soil type, and warns when rain or heat makes fertilizer application risky.

Weather and location lookups require an internet connection. If the live source is unavailable or the location cannot be found, the advisor shows an error rather than substituting generic or stale weather. Fertilizer rates are not guessed: exact quantities require a current soil test and the relevant district agriculture department or university crop package. The guidance is decision support, not a substitute for local agronomic advice.

## Notes

This is a professional demo project, not a production-grade agricultural certification or scientific guarantee system. Product claims and recommended application rates are presented as configurable demo content rather than verified agronomic guarantees.
