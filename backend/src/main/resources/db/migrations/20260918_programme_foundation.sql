-- Additive, PostgreSQL-only foundation. Legacy year-based settings remain authoritative
-- until programme management is implemented. Run the whole file in one transaction.
CREATE TABLE IF NOT EXISTS programmes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL
);
INSERT INTO programmes(code,name) VALUES
 ('bangla-karneval','Bangla Karneval'), ('eid','Eid'), ('puja','Puja'),
 ('bangla-noboborsho','Bangla Noboborsho'), ('bbq','BBQ'), ('game','Game')
ON CONFLICT(code) DO NOTHING;

CREATE TABLE IF NOT EXISTS organisation_profile (
    id INTEGER PRIMARY KEY CHECK(id=1),
    name VARCHAR(150) NOT NULL,
    story TEXT,
    story_source_event_year INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO organisation_profile(id,name,story,story_source_event_year)
SELECT 1,'Bangla Karneval e.V.',c.about_text,c.event_year
FROM application_settings s LEFT JOIN event_config c ON c.event_year=s.active_event_year
WHERE s.id=1 ON CONFLICT(id) DO NOTHING;
INSERT INTO organisation_profile(id,name) VALUES(1,'Bangla Karneval e.V.') ON CONFLICT(id) DO NOTHING;

CREATE TABLE IF NOT EXISTS event_editions (
    id BIGSERIAL PRIMARY KEY,
    programme_id BIGINT NOT NULL REFERENCES programmes(id) ON DELETE RESTRICT,
    slug VARCHAR(180) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    event_year INTEGER NOT NULL,
    -- Only migrated Bangla Karneval editions use this compatibility key.
    legacy_event_year INTEGER UNIQUE,
    event_date DATE,
    event_location VARCHAR(255),
    description TEXT,
    price_per_person NUMERIC(10,2),
    registration_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    performer_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    theme_key VARCHAR(60) NOT NULL,
    poster_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(id,event_year)
);
CREATE INDEX IF NOT EXISTS idx_event_editions_programme_year ON event_editions(programme_id,event_year);

-- Include historical years present only in media or registration records.
INSERT INTO event_editions(programme_id,slug,title,event_year,legacy_event_year,theme_key,poster_path)
SELECT p.id,'bangla-karneval-'||y.event_year,'Bangla Karneval '||y.event_year,
 y.event_year,y.event_year,'bangla-karneval','/assets/images/home_background.jpg'
FROM (
 SELECT event_year FROM event_config UNION SELECT event_year FROM events
 UNION SELECT event_year FROM registrations UNION SELECT event_year FROM performer_registrations
 UNION SELECT event_year FROM gallery_items UNION SELECT active_event_year FROM application_settings
) y CROSS JOIN programmes p WHERE p.code='bangla-karneval' AND y.event_year IS NOT NULL
ON CONFLICT(legacy_event_year) DO NOTHING;

UPDATE event_editions e SET event_date=c.event_date,event_location=c.event_location,
 description=c.about_text,price_per_person=c.price_per_person,
 registration_enabled=COALESCE(c.registration_enabled,FALSE),performer_enabled=COALESCE(c.performer_enabled,FALSE)
FROM event_config c WHERE e.legacy_event_year=c.event_year;

-- New legacy event years and edits keep their edition settings in sync.
CREATE OR REPLACE FUNCTION sync_legacy_event_edition() RETURNS TRIGGER LANGUAGE plpgsql AS $fn$
BEGIN
 INSERT INTO event_editions(programme_id,slug,title,event_year,legacy_event_year,
  event_date,event_location,description,price_per_person,registration_enabled,performer_enabled,theme_key,poster_path)
 SELECT id,'bangla-karneval-'||NEW.event_year,'Bangla Karneval '||NEW.event_year,
  NEW.event_year,NEW.event_year,NEW.event_date,NEW.event_location,NEW.about_text,NEW.price_per_person,
  COALESCE(NEW.registration_enabled,FALSE),COALESCE(NEW.performer_enabled,FALSE),'bangla-karneval','/assets/images/home_background.jpg'
 FROM programmes WHERE code='bangla-karneval'
 ON CONFLICT(legacy_event_year) DO UPDATE SET event_date=EXCLUDED.event_date,event_location=EXCLUDED.event_location,
  description=EXCLUDED.description,price_per_person=EXCLUDED.price_per_person,
  registration_enabled=EXCLUDED.registration_enabled,performer_enabled=EXCLUDED.performer_enabled;
 RETURN NEW;
END $fn$;
DROP TRIGGER IF EXISTS sync_legacy_event_edition ON event_config;
CREATE TRIGGER sync_legacy_event_edition AFTER INSERT OR UPDATE ON event_config
FOR EACH ROW EXECUTE FUNCTION sync_legacy_event_edition();

CREATE OR REPLACE FUNCTION link_legacy_event_record() RETURNS TRIGGER LANGUAGE plpgsql AS $fn$
BEGIN
 IF TG_OP='UPDATE' THEN
  IF NEW.event_year IS DISTINCT FROM OLD.event_year AND NEW.event_edition_id IS NOT DISTINCT FROM OLD.event_edition_id THEN
   NEW.event_edition_id=NULL;
  END IF;
 END IF;
 IF NEW.event_edition_id IS NULL AND NEW.event_year IS NOT NULL THEN
  INSERT INTO event_editions(programme_id,slug,title,event_year,legacy_event_year,theme_key,poster_path)
  SELECT id,'bangla-karneval-'||NEW.event_year,'Bangla Karneval '||NEW.event_year,
   NEW.event_year,NEW.event_year,'bangla-karneval','/assets/images/home_background.jpg'
  FROM programmes WHERE code='bangla-karneval' ON CONFLICT(legacy_event_year) DO NOTHING;
  SELECT id INTO NEW.event_edition_id FROM event_editions WHERE legacy_event_year=NEW.event_year;
 END IF;
 RETURN NEW;
END $fn$;

DO $migration$
DECLARE tbl TEXT;
BEGIN
 FOREACH tbl IN ARRAY ARRAY['event_config','events','registrations','performer_registrations','gallery_items'] LOOP
  EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS event_edition_id BIGINT',tbl);
  EXECUTE format('UPDATE %I r SET event_edition_id=e.id FROM event_editions e WHERE r.event_year=e.legacy_event_year AND r.event_edition_id IS NULL',tbl);
  IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid=to_regclass(tbl) AND conname=tbl||'_edition_fk') THEN
   EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY(event_edition_id,event_year) REFERENCES event_editions(id,event_year) ON DELETE RESTRICT',tbl,tbl||'_edition_fk');
  END IF;
  EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I(event_edition_id)',tbl||'_edition_idx',tbl);
  EXECUTE format('DROP TRIGGER IF EXISTS link_legacy_event_record ON %I',tbl);
  EXECUTE format('CREATE TRIGGER link_legacy_event_record BEFORE INSERT OR UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION link_legacy_event_record()',tbl);
 END LOOP;
END $migration$;

ALTER TABLE application_settings ADD COLUMN IF NOT EXISTS active_event_edition_id BIGINT REFERENCES event_editions(id) ON DELETE RESTRICT;
UPDATE application_settings s SET active_event_edition_id=e.id FROM event_editions e
WHERE s.active_event_year=e.legacy_event_year AND s.active_event_edition_id IS NULL;
CREATE OR REPLACE FUNCTION sync_legacy_active_edition() RETURNS TRIGGER LANGUAGE plpgsql AS $fn$
BEGIN
 SELECT id INTO NEW.active_event_edition_id FROM event_editions WHERE legacy_event_year=NEW.active_event_year;
 IF NEW.active_event_edition_id IS NULL THEN
  RAISE EXCEPTION 'No edition exists for the selected legacy event year %',NEW.active_event_year;
 END IF;
 RETURN NEW;
END $fn$;
DROP TRIGGER IF EXISTS sync_legacy_active_edition ON application_settings;
CREATE TRIGGER sync_legacy_active_edition BEFORE INSERT OR UPDATE OF active_event_year ON application_settings
FOR EACH ROW EXECUTE FUNCTION sync_legacy_active_edition();
