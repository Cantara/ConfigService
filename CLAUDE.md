# ConfigService

## Purpose
Server component of the Cantara controlled application instance regime. Manages software upgrades and corresponding configurations for home-built Java applications and services. Designed to solve the update and configuration pain-point in microservice architectures and for client installs of homegrown applications.

## Tech Stack
- Language: Java 8+
- Framework: Jersey 2.x, Jetty 9.x, Spring 5.x
- Build: Maven
- Key dependencies: ConfigService-SDK, PostgreSQL (or embedded Postgres), Flyway, HikariCP, AWS SDK, Constretto, Jackson
- Persistence: PostgreSQL (production) or Embedded PostgreSQL (development)

## Architecture
Standalone microservice that serves as a centralized configuration registry. Applications query ConfigService for their configuration and download artifacts. Supports Docker deployment (Alpine, Ubuntu, AWS variants). Uses a REST API for application registration, configuration management, and client updates. Pairs with Java-Auto-Update (JAU) on the client side.

## Key Entry Points
- `src/main/resources/application.properties` - Main configuration
- REST API endpoints: `/jau/application`, `/jau/health`, `/jau/serviceconfig/query`
- Docker configurations in `DockerAlpine/`, `Docker/`, `DockerAWS/`

## Development
```bash
# Build
mvn clean install

# Test (default suite)
mvn test

# Test with Postgres
mvn test -Dtestnames.to.run=default,postgres

# Run
java -jar target/configservice-*.jar
```

## Domain Context
Application lifecycle management and configuration distribution. Central to the Cantara deployment infrastructure, enabling zero-downtime upgrades and centralized configuration management across distributed microservice deployments.
