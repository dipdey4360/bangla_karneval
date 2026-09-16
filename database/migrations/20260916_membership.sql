-- Safe to run against an existing database. Does not modify existing event data.
CREATE TABLE IF NOT EXISTS board_members (
    id INTEGER PRIMARY KEY CHECK (id BETWEEN 1 AND 6),
    name VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    image_url VARCHAR(500)
);
CREATE TABLE IF NOT EXISTS members (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    partner_name VARCHAR(100),
    date_of_birth DATE NOT NULL,
    address VARCHAR(500) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(150) NOT NULL,
    membership_type VARCHAR(20) NOT NULL,
    annual_fee NUMERIC(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_declared BOOLEAN NOT NULL DEFAULT FALSE,
    payment_verified BOOLEAN NOT NULL DEFAULT FALSE,
    consent_at TIMESTAMP NOT NULL,
    consent_text VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    listed BOOLEAN NOT NULL DEFAULT TRUE,
    admin_note VARCHAR(2000),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    email_delivery VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUESTED'
);
ALTER TABLE event_config ADD COLUMN IF NOT EXISTS membership_benefits TEXT;
ALTER TABLE event_config ADD COLUMN IF NOT EXISTS membership_single_fee NUMERIC(10,2);
ALTER TABLE event_config ADD COLUMN IF NOT EXISTS membership_couple_fee NUMERIC(10,2);
ALTER TABLE event_config ADD COLUMN IF NOT EXISTS membership_payment_instructions TEXT;
CREATE INDEX IF NOT EXISTS idx_members_status_listed ON members(status, listed);
