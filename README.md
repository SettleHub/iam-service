# SettleHub IAM Service

![Java](https://img.shields.io/badge/Java-21-blue?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3+-brightgreen?logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Ready-blue?logo=postgresql)
![Apache Kafka](https://img.shields.io/badge/Kafka-Event_Driven-black?logo=apachekafka)

The Identity and Access Management (IAM) microservice for the SettleHub ecosystem. This service provides centralized authentication, authorization, user management, and secure Machine-to-Machine (M2M) communication.

## Core Features

* **JWT Authentication:** Implements stateless Bearer token authorization with short-lived Access Tokens and long-lived Refresh Tokens.
* **Role-Based Access Control (RBAC):** Manages user roles (e.g., `ROLE_USER`, `ROLE_ADMIN`, `ROLE_MODERATOR`).
* **Event-Driven Architecture:** Publishes asynchronous user lifecycle events (registration, email verification, password resets) to **Apache Kafka**.
* **M2M Internal API:** Exposes dedicated, network-isolated endpoints for other SettleHub microservices to fetch user data securely.
* **Health Monitoring:** Integrated with `SettleHub/health-common` for liveness probes and heartbeat streaming.

## Prerequisites

Before running this service, ensure you have the following running in your environment:
* Java 21+
* PostgreSQL (Port `5432`)
* Apache Kafka (Port `9092`)

## Configuration

The service relies on environment variables for sensitive data. Create a `.env` file in the root directory of the project:

```env
# Database Configuration
IAM_DB_URL=jdbc:postgresql://localhost:5432/iam-service-db
IAM_DB_USERNAME=postgres
IAM_DB_PASSWORD=your_secure_password

# JWT Security
# Must be a highly secure Base64 encoded string
JWT_SECRET_KEY=your_base64_encoded_secret_key_here

# Kafka Configuration
KAFKA_ADDRESS=localhost:9092
```

## Running the Application

You can easily run the application using the provided Maven wrapper:

```bash
# Clean and build the project
./mvnw clean install

# Run the Spring Boot application
./mvnw spring-boot:run
```

The service will start on http://localhost:8000/iam/api.

## API Documentation

The IAM Service provides two distinct API layers:

**Public API (`/identity/**`, `/users/**`)**: For user authentication, registration, and session management.

**Internal M2M API (`/internal/users/**`)**: For secure, network-isolated communication with other SettleHub microservices.

👉 **[Read the complete API Reference here](docs/API.md)**

> [!Note]
> When the application is running locally, you can also explore the interactive OpenAPI/Swagger UI at `http://localhost:8000/iam/api/swagger-ui.html` (if enabled).*

## Security Notes & Roadmap

**Data Encryption**: This service architecture is designed to be integrated with Cossack Labs Acra for advanced database-level encryption (Transparent Data Encryption), ensuring user credentials and PII are securely encrypted before reaching the database.

**Token Blacklisting**: Currently, logout relies on client-side token disposal (removing the token from local storage). Future iterations will include a Redis-based server-side token blacklisting mechanism to forcefully invalidate compromised JWTs before their expiration time.

