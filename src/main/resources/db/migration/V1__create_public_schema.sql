CREATE TABLE IF NOT EXISTS tenants (
    id            BIGSERIAL PRIMARY KEY,
    shop_name     VARCHAR(100) NOT NULL,
    slug          VARCHAR(50)  UNIQUE NOT NULL,
    owner_email   VARCHAR(100) UNIQUE NOT NULL,
    schema_name   VARCHAR(60)  UNIQUE NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    trial_ends_at TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS super_admin_users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);