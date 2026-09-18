-- Edition IDs now select the active event. Legacy year endpoints remain Bangla-only.
DROP TRIGGER IF EXISTS sync_legacy_active_edition ON application_settings;
ALTER TABLE event_editions ADD COLUMN IF NOT EXISTS tagline VARCHAR(500);
ALTER TABLE event_editions ADD COLUMN IF NOT EXISTS accent_color VARCHAR(7) DEFAULT '#7B241C';
ALTER TABLE event_editions ADD COLUMN IF NOT EXISTS payment_instructions TEXT;
ALTER TABLE event_editions ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(30);
ALTER TABLE event_editions ADD COLUMN IF NOT EXISTS contact_email VARCHAR(150);
ALTER TABLE event_editions ADD COLUMN IF NOT EXISTS contact_whatsapp VARCHAR(30);
ALTER TABLE event_editions ADD COLUMN IF NOT EXISTS contact_facebook VARCHAR(255);
ALTER TABLE event_editions ADD COLUMN IF NOT EXISTS contact_instagram VARCHAR(255);
ALTER TABLE event_editions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
UPDATE event_editions e SET tagline='Celebrating Bengali culture, heritage & community',
 contact_phone=to_jsonb(c)->>'contact_phone',contact_email=to_jsonb(c)->>'contact_email',
 contact_whatsapp=to_jsonb(c)->>'contact_whatsapp',contact_facebook=to_jsonb(c)->>'contact_facebook',
 contact_instagram=to_jsonb(c)->>'contact_instagram'
FROM event_config c WHERE e.legacy_event_year=c.event_year;
ALTER TABLE organisation_profile ADD COLUMN IF NOT EXISTS contact_email VARCHAR(150);
ALTER TABLE organisation_profile ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(30);
ALTER TABLE organisation_profile ADD COLUMN IF NOT EXISTS contact_facebook VARCHAR(255);
ALTER TABLE organisation_profile ADD COLUMN IF NOT EXISTS contact_instagram VARCHAR(255);
UPDATE organisation_profile o SET contact_email=e.contact_email,contact_phone=e.contact_phone,
 contact_facebook=e.contact_facebook,contact_instagram=e.contact_instagram
FROM application_settings s JOIN event_editions e ON e.id=s.active_event_edition_id WHERE o.id=1;
-- Old foreign keys to event_config(event_year) cannot identify other programmes.
DO $migration$
DECLARE r RECORD;
BEGIN
 FOR r IN SELECT c.conname,c.conrelid::regclass AS tbl FROM pg_constraint c
 WHERE c.contype='f' AND c.confrelid='event_config'::regclass
 AND c.conrelid IN ('events'::regclass,'registrations'::regclass,'performer_registrations'::regclass,'gallery_items'::regclass)
 LOOP EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',r.tbl,r.conname); END LOOP;
END $migration$;

DO $snapshots$
DECLARE tbl TEXT;
BEGIN
 FOREACH tbl IN ARRAY ARRAY['registrations','performer_registrations'] LOOP
 EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS event_title VARCHAR(255)',tbl);
 EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS event_date_snapshot DATE',tbl);
 EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS event_location_snapshot VARCHAR(255)',tbl);
 EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS payment_instructions_snapshot TEXT',tbl);
 EXECUTE format('UPDATE %I r SET event_title=e.title,event_date_snapshot=e.event_date,event_location_snapshot=e.event_location FROM event_editions e WHERE r.event_edition_id=e.id AND r.event_title IS NULL',tbl);
 END LOOP;
END $snapshots$;

CREATE OR REPLACE FUNCTION bump_edition_version() RETURNS TRIGGER LANGUAGE plpgsql AS $fn$
BEGIN NEW.version=OLD.version+1; RETURN NEW; END $fn$;
DROP TRIGGER IF EXISTS bump_edition_version ON event_editions;
CREATE TRIGGER bump_edition_version BEFORE UPDATE ON event_editions FOR EACH ROW EXECUTE FUNCTION bump_edition_version();
DO $fk$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='application_settings'::regclass AND conname='active_edition_fk') THEN
  ALTER TABLE application_settings ADD CONSTRAINT active_edition_fk FOREIGN KEY(active_event_edition_id) REFERENCES event_editions(id) ON DELETE RESTRICT;
 END IF;
END $fk$;
