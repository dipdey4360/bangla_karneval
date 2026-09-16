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
