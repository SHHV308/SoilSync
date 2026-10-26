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

## Production hosting for https://soilsync.in

This project is ready to deploy behind a reverse proxy to a live domain such as `https://soilsync.in`, but the actual live deployment requires access to your hosting account, DNS records, and TLS certificate management.

Recommended deployment flow:

1. Build the application JAR:

```bash
mvn clean package
```

2. Run the built app on the server:

```bash
java -jar target/SoilSync-0.0.1-SNAPSHOT.jar
```

3. Point Nginx or Caddy at the app on port 8080 and enable HTTPS for `soilsync.in`.

4. If you are using a reverse proxy, keep `server.forward-headers-strategy=framework` enabled so redirects and generated URLs work correctly behind SSL termination.

Example Nginx snippet:

```nginx
server {
    listen 80;
    server_name soilsync.in www.soilsync.in;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name soilsync.in www.soilsync.in;

    ssl_certificate /etc/letsencrypt/live/soilsync.in/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/soilsync.in/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## Login

The demo app allows anonymous browsing. The UI does not require an account for the local demonstration.

## Regional crop advice

The advisor asks for an Indian town/district, crop, growth stage, and soil type. It geocodes the location and retrieves current conditions plus a three-day forecast from the public Open-Meteo APIs. It combines that live weather with crop-stage guidance and the selected soil type, and warns when rain or heat makes fertilizer application risky.

Weather and location lookups require an internet connection. If the live source is unavailable or the location cannot be found, the advisor shows an error rather than substituting generic or stale weather. Fertilizer rates are not guessed: exact quantities require a current soil test and the relevant district agriculture department or university crop package. The guidance is decision support, not a substitute for local agronomic advice.

## Notes

This is a professional demo project, not a production-grade agricultural certification or scientific guarantee system. Product claims and recommended application rates are presented as configurable demo content rather than verified agronomic guarantees.
