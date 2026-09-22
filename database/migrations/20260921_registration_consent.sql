-- Keep historical registrations without consent evidence unchanged.
ALTER TABLE registrations ADD COLUMN IF NOT EXISTS consent_at TIMESTAMP;
ALTER TABLE registrations ADD COLUMN IF NOT EXISTS consent_text VARCHAR(500);
