ALTER TABLE members ADD COLUMN IF NOT EXISTS membership_starts_on DATE;
ALTER TABLE members ADD COLUMN IF NOT EXISTS membership_expires_on DATE;
ALTER TABLE members ADD COLUMN IF NOT EXISTS expiry_reminder_sent_at TIMESTAMP;
ALTER TABLE members ADD COLUMN IF NOT EXISTS partner_expiry_reminder_sent_at TIMESTAMP;
-- Use the recorded approval date, never today's migration date or the submission date.
UPDATE members m SET membership_starts_on = (to_jsonb(m)->>'reviewed_at')::timestamp::date
WHERE to_jsonb(m)->>'status' = 'APPROVED' AND membership_starts_on IS NULL
  AND to_jsonb(m)->>'reviewed_at' IS NOT NULL;
UPDATE members SET membership_expires_on = (membership_starts_on + INTERVAL '1 year')::date
WHERE membership_starts_on IS NOT NULL AND membership_expires_on IS NULL;
CREATE INDEX IF NOT EXISTS members_expiry_reminder_idx ON members(membership_expires_on);
