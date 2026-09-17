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
