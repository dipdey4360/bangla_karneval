-- Preserve historical applications without recorded consent.
ALTER TABLE performer_registrations ADD COLUMN IF NOT EXISTS consent_at TIMESTAMP;
ALTER TABLE performer_registrations ADD COLUMN IF NOT EXISTS consent_text VARCHAR(500);
