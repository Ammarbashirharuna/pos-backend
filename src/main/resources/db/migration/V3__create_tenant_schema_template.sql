-- This migration updates the tenants table with new columns
-- Actual tenant schemas are created dynamically on registration

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS email_verified_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS verification_token VARCHAR(100),
    ADD COLUMN IF NOT EXISTS owner_name         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS phone              VARCHAR(20);

-- Function to create a tenant schema with all required tables.
-- Called by Spring Boot when a new tenant registers.
CREATE OR REPLACE FUNCTION create_tenant_schema(schema_name TEXT)
RETURNS VOID AS $$
BEGIN
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.users (
            id                      BIGSERIAL PRIMARY KEY,
            username                VARCHAR(50)  NOT NULL,
            full_name               VARCHAR(100) NOT NULL,
            email                   VARCHAR(100) NOT NULL UNIQUE,
            password_hash           VARCHAR(255) NOT NULL,
            role                    VARCHAR(20)  NOT NULL DEFAULT ''CASHIER'',
            is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
            failed_login_attempts   INTEGER      NOT NULL DEFAULT 0,
            locked_until            TIMESTAMP,
            last_login              TIMESTAMP,
            password_reset_token    VARCHAR(100),
            password_reset_expires_at TIMESTAMP,
            refresh_token_hash      VARCHAR(255),
            created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
            updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
        )', schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.settings (
            id              INTEGER PRIMARY KEY DEFAULT 1,
            shop_name       VARCHAR(100) NOT NULL,
            address         TEXT,
            phone           VARCHAR(20),
            email           VARCHAR(100),
            logo_url        VARCHAR(500),
            tax_rate        NUMERIC(5,2) NOT NULL DEFAULT 0.00,
            currency        VARCHAR(10)  NOT NULL DEFAULT ''NGN'',
            receipt_footer  TEXT,
            updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
        )', schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.categories (
            id          BIGSERIAL PRIMARY KEY,
            name        VARCHAR(100) NOT NULL,
            description TEXT,
            created_at  TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.products (
            id          BIGSERIAL PRIMARY KEY,
            name        VARCHAR(200) NOT NULL,
            category_id BIGINT REFERENCES %I.categories(id),
            price       NUMERIC(12,2) NOT NULL,
            cost_price  NUMERIC(12,2),
            stock       INTEGER NOT NULL DEFAULT 0,
            min_stock   INTEGER NOT NULL DEFAULT 5,
            barcode     VARCHAR(100) UNIQUE,
            description TEXT,
            image_url   VARCHAR(500),
            is_active   BOOLEAN NOT NULL DEFAULT TRUE,
            created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name, schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.customers (
            id                  BIGSERIAL PRIMARY KEY,
            name                VARCHAR(100) NOT NULL,
            phone               VARCHAR(20),
            email               VARCHAR(100),
            address             TEXT,
            credit_limit        NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            outstanding_balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            created_at          TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.sales (
            id              BIGSERIAL PRIMARY KEY,
            invoice_no      VARCHAR(50) NOT NULL UNIQUE,
            customer_id     BIGINT REFERENCES %I.customers(id),
            subtotal        NUMERIC(12,2) NOT NULL,
            tax_amount      NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            total_amount    NUMERIC(12,2) NOT NULL,
            payment_method  VARCHAR(20)  NOT NULL,
            amount_paid     NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            change_amount   NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            cashier_id      BIGINT NOT NULL,
            notes           VARCHAR(255),
            created_at      TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name, schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.sale_items (
            id           BIGSERIAL PRIMARY KEY,
            sale_id      BIGINT NOT NULL REFERENCES %I.sales(id),
            product_id   BIGINT NOT NULL,
            product_name VARCHAR(200) NOT NULL,
            quantity     INTEGER NOT NULL,
            unit_price   NUMERIC(12,2) NOT NULL,
            subtotal     NUMERIC(12,2) NOT NULL,
            created_at   TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name, schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.payments_received (
            id             BIGSERIAL PRIMARY KEY,
            customer_id    BIGINT NOT NULL REFERENCES %I.customers(id),
            sale_id        BIGINT REFERENCES %I.sales(id),
            amount         NUMERIC(12,2) NOT NULL,
            payment_method VARCHAR(20) NOT NULL,
            note           TEXT,
            received_by    BIGINT NOT NULL,
            created_at     TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name, schema_name, schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.stock_history (
            id              BIGSERIAL PRIMARY KEY,
            product_id      BIGINT NOT NULL,
            quantity_change INTEGER NOT NULL,
            type            VARCHAR(30) NOT NULL,
            reason          TEXT,
            user_id         BIGINT NOT NULL,
            reference_id    BIGINT,
            created_at      TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.suppliers (
            id         BIGSERIAL PRIMARY KEY,
            name       VARCHAR(100) NOT NULL,
            phone      VARCHAR(20),
            email      VARCHAR(100),
            address    TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.purchase_orders (
            id           BIGSERIAL PRIMARY KEY,
            supplier_id  BIGINT NOT NULL REFERENCES %I.suppliers(id),
            status       VARCHAR(20) NOT NULL DEFAULT ''PENDING'',
            total_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            received_by  BIGINT,
            ordered_at   TIMESTAMP NOT NULL DEFAULT NOW(),
            received_at  TIMESTAMP
        )', schema_name, schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.purchase_items (
            id                BIGSERIAL PRIMARY KEY,
            po_id             BIGINT NOT NULL REFERENCES %I.purchase_orders(id),
            product_id        BIGINT NOT NULL,
            quantity_ordered  INTEGER NOT NULL,
            quantity_received INTEGER NOT NULL DEFAULT 0,
            unit_cost         NUMERIC(12,2) NOT NULL,
            created_at        TIMESTAMP NOT NULL DEFAULT NOW()
        )', schema_name, schema_name);

END;
$$ LANGUAGE plpgsql;