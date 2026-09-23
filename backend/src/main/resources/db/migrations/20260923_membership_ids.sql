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
