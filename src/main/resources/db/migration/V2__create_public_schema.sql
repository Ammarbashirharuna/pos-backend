CREATE TABLE IF NOT EXISTS plans (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50)    NOT NULL,
    price_monthly   NUMERIC(12,2)  NOT NULL,
    max_users       INTEGER        NOT NULL,
    max_branches    INTEGER        NOT NULL,
    features_json   TEXT,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT         NOT NULL REFERENCES tenants(id),
    plan_id         BIGINT         NOT NULL REFERENCES plans(id),
    paystack_sub_code VARCHAR(100),
    status          VARCHAR(20)    NOT NULL DEFAULT 'TRIAL',
    period_start    TIMESTAMP,
    period_end      TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payments (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT         NOT NULL REFERENCES tenants(id),
    amount          NUMERIC(12,2)  NOT NULL,
    paystack_ref    VARCHAR(100)   UNIQUE,
    status          VARCHAR(20)    NOT NULL,
    paid_at         TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL PRIMARY KEY,
    admin_id        BIGINT,
    tenant_id       BIGINT,
    action          VARCHAR(100)   NOT NULL,
    details         TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS announcements (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200)   NOT NULL,
    message         TEXT           NOT NULL,
    severity        VARCHAR(20)    NOT NULL DEFAULT 'INFO',
    starts_at       TIMESTAMP,
    ends_at         TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Seed default plans
INSERT INTO plans (name, price_monthly, max_users, max_branches, features_json)
VALUES
  ('Basic',    8000,  2, 1, '{"reports":"basic","purchase_orders":false,"statements":false}'),
  ('Standard', 15000, 5, 1, '{"reports":"all","purchase_orders":true,"statements":true}'),
  ('Pro',      25000, 999, 3, '{"reports":"all","purchase_orders":true,"statements":true,"priority_support":true}')
ON CONFLICT DO NOTHING;