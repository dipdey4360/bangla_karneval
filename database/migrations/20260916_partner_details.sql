-- Optional explicit migration; Hibernate ddl-auto=update also adds these columns.
-- Existing applications remain intact; legacy partner details are nullable.
ALTER TABLE members ADD COLUMN IF NOT EXISTS partner_date_of_birth DATE;
ALTER TABLE members ADD COLUMN IF NOT EXISTS partner_address VARCHAR(500);
ALTER TABLE members ADD COLUMN IF NOT EXISTS partner_phone VARCHAR(30);
ALTER TABLE members ADD COLUMN IF NOT EXISTS partner_email VARCHAR(150);
