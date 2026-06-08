-- V3__add_password_reset_columns.sql
-- Adds password reset fields to every existing tenant schema
-- New tenants pick this up automatically via Flyway on schema creation

-- Run this once manually on any tenant schemas that already exist in your DB.
-- Example: if you have tenant_test_shop already created, connect to it and run:
--   SET search_path TO tenant_test_shop;
--   ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_token   VARCHAR(255);
--   ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_expires  TIMESTAMP;
--   ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_used        BOOLEAN DEFAULT FALSE;

-- This file is the Flyway migration for the TENANT schema template.
-- Place it in: src/main/resources/db/migration/tenant/V3__add_password_reset_columns.sql
-- (adjust the path to wherever your tenant Flyway migrations live)

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS password_reset_expires  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reset_token_used        BOOLEAN DEFAULT FALSE;