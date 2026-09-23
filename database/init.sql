CREATE TABLE IF NOT EXISTS admin_users (
                                           id            BIGSERIAL    PRIMARY KEY,
                                           name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  DEFAULT 'ADMIN',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS event_config (
                                            id                BIGSERIAL     PRIMARY KEY,
                                            event_year        INT           NOT NULL UNIQUE,
                                            price_per_person  NUMERIC(10,2) DEFAULT 10.00,
    event_date        DATE,
    event_location    VARCHAR(255),
    about_text        TEXT,
    contact_phone     VARCHAR(30),
    contact_email     VARCHAR(150),
    contact_whatsapp  VARCHAR(30),
    contact_facebook  VARCHAR(255),
    contact_instagram VARCHAR(255),
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS events (
                                      id           BIGSERIAL    PRIMARY KEY,
                                      event_year   INT          NOT NULL,
                                      category     VARCHAR(50),
    title        VARCHAR(255),
    description  TEXT,
    is_highlight BOOLEAN      DEFAULT FALSE,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_year) REFERENCES event_config(event_year)
    );

CREATE TABLE IF NOT EXISTS registrations (
                                             id                    BIGSERIAL     PRIMARY KEY,
                                             primary_name          VARCHAR(100)  NOT NULL,
    email                 VARCHAR(150)  NOT NULL,
    primary_date_of_birth DATE          NOT NULL,
    gender                VARCHAR(20),
    address               TEXT,
    phone                 VARCHAR(30),
    participant_count     INT           DEFAULT 1 CHECK (participant_count >= 1),
    calculated_amount     NUMERIC(10,2) NOT NULL,
    payment_method        VARCHAR(20),
    payment_status        VARCHAR(20)   DEFAULT 'PENDING',
    reference_code        VARCHAR(50)   UNIQUE,
    admin_note            TEXT,                          -- ← ADDED (was missing)
    event_year            INT,
    registered_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    consent_at            TIMESTAMP,
    consent_text          VARCHAR(500),
    FOREIGN KEY (event_year) REFERENCES event_config(event_year)
    );

CREATE INDEX IF NOT EXISTS idx_registrations_name   ON registrations(primary_name);
CREATE INDEX IF NOT EXISTS idx_registrations_status ON registrations(payment_status);
CREATE INDEX IF NOT EXISTS idx_registrations_year   ON registrations(event_year);

CREATE TABLE IF NOT EXISTS additional_participants (
                                                       id              BIGSERIAL    PRIMARY KEY,
                                                       registration_id BIGINT       NOT NULL,               -- ← FIXED (was BIGSERIAL)
                                                       name            VARCHAR(100),
    date_of_birth   DATE         NOT NULL,
    gender          VARCHAR(20),
    relation        VARCHAR(30),
    FOREIGN KEY (registration_id) REFERENCES registrations(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS performer_registrations (
                                                       id                      BIGSERIAL    PRIMARY KEY,
                                                       name                    VARCHAR(100) NOT NULL,
    email                   VARCHAR(150) NOT NULL,
    phone                   VARCHAR(30),
    date_of_birth           DATE,
    address                 TEXT,
    performance_type        VARCHAR(100),
    performance_description TEXT,
    group_member_count      INT          DEFAULT 1,
    approval_status         VARCHAR(20)  DEFAULT 'PENDING',
    admin_note              TEXT,                        -- ← ADDED (was missing)
    event_year              INT,
    registered_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    consent_at              TIMESTAMP,
    consent_text            VARCHAR(500),
    FOREIGN KEY (event_year) REFERENCES event_config(event_year)
    );

CREATE TABLE IF NOT EXISTS performer_group_members (
                                                       id                        BIGSERIAL    PRIMARY KEY,
                                                       performer_registration_id BIGINT       NOT NULL,     -- ← FIXED (was BIGSERIAL)
                                                       name                      VARCHAR(100) NOT NULL,
    date_of_birth             DATE,
    gender                    VARCHAR(20),
    FOREIGN KEY (performer_registration_id)
    REFERENCES performer_registrations(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS gallery_items (
                                             id            BIGSERIAL    PRIMARY KEY,
                                             event_year    INT          NOT NULL,
                                             media_type    VARCHAR(10),
    url           VARCHAR(500) NOT NULL,
    caption       VARCHAR(255),
    is_highlight  BOOLEAN      DEFAULT FALSE,
    display_order INT          DEFAULT 0,
    uploaded_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_year) REFERENCES event_config(event_year)
    );

CREATE INDEX IF NOT EXISTS idx_gallery_year      ON gallery_items(event_year);
CREATE INDEX IF NOT EXISTS idx_gallery_highlight ON gallery_items(is_highlight);

CREATE TABLE IF NOT EXISTS contact_inquiries (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    message      TEXT         NOT NULL,
    read         BOOLEAN      DEFAULT FALSE,
    answered     BOOLEAN      DEFAULT FALSE,
    reply_text   TEXT,
    replied_at   TIMESTAMP,
    submitted_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    );

ALTER TABLE event_config ADD COLUMN IF NOT EXISTS performer_enabled BOOLEAN DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS sponsors (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    address       VARCHAR(255),
    website_url   VARCHAR(500),
    phone         VARCHAR(30),
    description   VARCHAR(500),
    logo_path     VARCHAR(500),
    is_visible    BOOLEAN      DEFAULT TRUE,
    display_order INT          DEFAULT 0,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    );
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
    partner_date_of_birth DATE,
    partner_address VARCHAR(500),
    partner_phone VARCHAR(30),
    partner_email VARCHAR(150),
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
-- Retains the current event and migrates membership settings once.
CREATE TABLE IF NOT EXISTS application_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    active_event_year INTEGER NOT NULL,
    membership_benefits TEXT,
    membership_single_fee NUMERIC(10,2),
    membership_couple_fee NUMERIC(10,2),
    membership_payment_instructions TEXT
);
INSERT INTO application_settings (id, active_event_year, membership_benefits,
    membership_single_fee, membership_couple_fee, membership_payment_instructions)
SELECT 1, 2026, membership_benefits, membership_single_fee, membership_couple_fee,
    membership_payment_instructions FROM event_config WHERE event_year = 2026
ON CONFLICT (id) DO NOTHING;
INSERT INTO application_settings (id, active_event_year) VALUES (1, 2026)
ON CONFLICT (id) DO NOTHING;

CREATE SEQUENCE IF NOT EXISTS membership_number_seq;
ALTER TABLE members ADD COLUMN IF NOT EXISTS membership_id VARCHAR(40);
ALTER TABLE application_settings ADD COLUMN IF NOT EXISTS member_discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0;
-- Preserve existing IDs and never reuse a number after deletion.
SELECT setval('membership_number_seq', GREATEST((SELECT last_value FROM membership_number_seq),
    COALESCE((SELECT MAX(substring(membership_id FROM 5)::bigint) FROM members WHERE membership_id ~ '^BKM-[0-9]+$'),0)),
    (SELECT is_called FROM membership_number_seq) OR EXISTS(SELECT 1 FROM members WHERE membership_id IS NOT NULL));
DO $$
DECLARE row_id bigint; number_text text;
BEGIN
    FOR row_id IN SELECT id FROM members m WHERE to_jsonb(m)->>'status' = 'APPROVED' AND membership_id IS NULL ORDER BY id LOOP
        number_text := nextval('membership_number_seq')::text;
        UPDATE members SET membership_id = 'BKM-' || lpad(number_text, GREATEST(5,length(number_text)), '0') WHERE id=row_id;
    END LOOP;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS members_membership_id_unique ON members(membership_id);
ALTER TABLE registrations ADD COLUMN IF NOT EXISTS member_discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0;
