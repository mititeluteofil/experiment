# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

This is a Gradle project (wrapper v8.7). On Windows use `gradlew.bat`, on Linux/Mac use `./gradlew`.

```bash
gradlew.bat build          # Full build with tests
gradlew.bat test           # Run tests only (JUnit 5)
gradlew.bat bootRun        # Start the application (port 8080)
gradlew.bat clean build    # Clean rebuild
gradlew.bat test --tests "com.example.demo.SomeTest"           # Run a single test class
gradlew.bat test --tests "com.example.demo.SomeTest.methodName" # Run a single test method
```

## Tech Stack

- **Java 17** / **Spring Boot 3.2.5** / **Gradle 8.7**
- Spring Web MVC, Spring Data JPA, Spring Security, Spring AOP
- Spring Cloud 2023.0.1 (OpenFeign, Resilience4j circuit breaker, Eureka client, Config client, LoadBalancer)
- H2 in-memory database (development), PostgreSQL (production-ready dependency included)
- Lombok for boilerplate reduction
- Spring Boot Actuator for monitoring

## Architecture

Spring Boot microservice scaffold at `com.example.demo`. Entry point is `DemoApplication.java` with `@EnableFeignClients` enabled for declarative inter-service HTTP calls.

**Database:** H2 in-memory (`jdbc:h2:mem:demodb`), console at `/h2-console`. Hibernate `ddl-auto: update` auto-manages schema.

**Spring Cloud services** (Eureka, Config Server) are configured but disabled for standalone development. Resilience4j circuit breaker is configured with default settings (window size 10, 50% failure threshold).

**Actuator endpoints:** `/actuator/health`, `/actuator/info`, `/actuator/metrics`.

**Logging:** Root at INFO, `com.example.demo` at DEBUG.
