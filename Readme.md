# Sovent POS — Multi-Tenant SaaS Backend

[![Java](https://img.shields.io/badge/Java-17+-orange?logo=openjdk)](https://www.oracle.com/java/technologies/javase/jdk17-archive.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)
[![Status](https://img.shields.io/badge/Status-Production%20Ready-brightgreen)](https://github.com/Ammarbashirharuna/sovent-pos-backend)

Enterprise-grade multi-tenant SaaS Point of Sale platform designed specifically for Nigerian wholesale and retail businesses. Built with modern Java microservices architecture, offering scalability, security, and reliability for high-volume transaction processing.

**Smart POS. Limitless Scale. Built for Africa.**

## 📋 Overview

Sovent POS is a comprehensive backend API powering a complete point-of-sale ecosystem. Supporting **14 interconnected modules**, it provides wholesale businesses with inventory management, sales tracking, customer management, and real-time reporting capabilities.

### Key Characteristics

- 🏢 **Multi-Tenant Architecture** — Complete data isolation per tenant
- 🔐 **Enterprise Security** — JWT + refresh token + role-based access control
- 📊 **14 Integrated Modules** — Full business lifecycle coverage
- 🚀 **Scalable Design** — Handles thousands of concurrent users
- 💾 **Reliable Database** — PostgreSQL with automated Flyway migrations
- 🐳 **Containerized** — Docker support for local dev and production
- 📖 **API Documentation** — Auto-generated Swagger UI
- 🛡️ **Production Ready** — Comprehensive error handling and monitoring
- 🌍 **Nigerian Market Focus** — NGN currency, local tax rates, GSM integration

---

## 🎯 Use Cases

### Wholesale Businesses
- Bulk product management
- Multi-location inventory tracking
- Wholesale pricing tiers
- Bulk order processing

### Retail Chains
- Point of sale operations
- Real-time inventory sync
- Sales analytics and reporting
- Customer loyalty programs

### Distribution Centers
- Stock allocation
- Transfer management
- Supplier integration
- Shipment tracking

---

## 🏗️ Architecture

### System Design
┌─────────────────────────────────────────────────┐

│         Client Applications (Web/Mobile)        │

├─────────────────────────────────────────────────┤

│                  API Gateway                     │

├─────────────────────────────────────────────────┤

│  Spring Boot 3.5 - Spring Security - REST API   │

├────────────┬────────────┬──────────┬────────────┤

│   Auth     │ Products   │   POS    │  Reports   │

│   Module   │   Module   │  Module  │  Module    │

├────────────┴────────────┴──────────┴────────────┤

│        Spring Data JPA - Hibernate ORM           │

├─────────────────────────────────────────────────┤

│  PostgreSQL 15 (Multi-Tenant Schema Per Tenant)  │

├─────────────────────────────────────────────────┤

│      Flyway Migrations - Version Control         │

└─────────────────────────────────────────────────┘

### Database Architecture

**Multi-Tenant Pattern:** Schema-per-tenant isolation

```sql
-- Master Database (Admin)
├── tenants (tenant metadata)
├── subscriptions (billing info)
└── audit_logs (system-wide logging)

-- Tenant Databases (Per Company)
├── users (tenant-specific users)
├── products (tenant inventory)
├── sales (transaction records)
├── stock_history (audit trail)
├── customers (customer data)
└── ... (14 total modules)
```

### Authentication Flow

User Login

↓
JWT Access Token (15 min) + Refresh Token (7 days)

↓
Access Token in Authorization Header
Refresh Token in HttpOnly Cookie

↓
Automatic Refresh Before Expiry


