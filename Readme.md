# SaaS POS Backend

Multi-tenant Point of Sale platform for Nigerian wholesale businesses.  
Built with Spring Boot 3.5 · PostgreSQL · JWT · Flyway · Docker

## Stack
- **Backend:** Java 17, Spring Boot 3.5, Spring Security 6
- **Database:** PostgreSQL 15 (Docker locally, Railway in production)
- **Auth:** JWT access token (15min) + refresh token (7 days HttpOnly cookie)
- **Migrations:** Flyway
- **Docs:** Swagger UI at `/swagger-ui.html`

## Local Development

### Prerequisites
- Java 17+
- Docker Desktop
- Maven

### Start
```bash
# Start database
docker-compose up -d

# Run app
mvn spring-boot:run
```

### URLs
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/api/health

## Environment Variables
See `application.yml` for all config keys.  
Production secrets are stored in Railway — never committed to git.

## Branch Strategy
- `main` — production only
- `develop` — integration branch
- `feature/module-name` — one branch per module
h
## Modules
14 modules — Auth, Dashboard, Products, Categories, POS, Sales,  
Stock, Customers, Users, Reports, Settings, Registration, Billing, Super Admin