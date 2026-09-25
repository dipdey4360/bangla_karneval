-- Board IDs are derived from the six fixed board positions. Ordinary memberships
-- retain their approval dates, expiry dates and donation status when renumbered.
ALTER TABLE members ADD COLUMN IF NOT EXISTS previous_membership_id VARCHAR(40);
LOCK TABLE members IN EXCLUSIVE MODE;
SELECT setval('membership_number_seq', GREATEST(6,
    (SELECT last_value FROM membership_number_seq),
    COALESCE((SELECT MAX(substring(membership_id FROM 5)::bigint)
        FROM members WHERE membership_id ~ '^BKM-[0-9]+$'), 0)), true);
DO $$
DECLARE member_row record; number_text text;
BEGIN
    FOR member_row IN SELECT id, membership_id FROM members
        WHERE membership_id ~ '^BKM-0000[1-6]$' ORDER BY membership_id LOOP
        number_text := nextval('membership_number_seq')::text;
        UPDATE members SET previous_membership_id=member_row.membership_id,
            membership_id='BKM-' || lpad(number_text, GREATEST(5,length(number_text)), '0')
        WHERE id=member_row.id;
    END LOOP;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='members'::regclass
        AND conname='members_board_ids_reserved') THEN
        ALTER TABLE members ADD CONSTRAINT members_board_ids_reserved
            CHECK (membership_id !~ '^BKM-0000[1-6]$');
    END IF;
END $$;
