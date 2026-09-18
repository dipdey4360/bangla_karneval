# Point 2 — Programme database foundation

This document records the point 2 database foundation. Points 3–7 have now been implemented and deployed locally; see [Programme management](PROGRAMME_MANAGEMENT.md) for current instructions. The transition restrictions below describe the earlier point 2 state. The current application uses event edition IDs for settings, activation, registrations, performers, activities and gallery records.

## Added structure

- `programmes`: Bangla Karneval, Eid, Puja, Bangla Noboborsho, BBQ and Game. Stable codes are separate from display names.
- `event_editions`: an independent ID and unique slug for each occurrence, with programme, year, title, date, venue, description, price, registration flags, theme key and poster path. The same programme can have multiple editions in one year.
- `organisation_profile`: the Bangla Karneval e.V. name and a one-time copy of the selected year's organisation story. The source year is recorded. Later event changes do not overwrite this copy. Historical stories also remain in their original event settings.
- `event_edition_id` on event settings, activities, registrations, performers and gallery items. Additional attendees and performer group members retain their existing parent relationships.
- `active_event_edition_id` on application settings, alongside the existing active year.
- `application_schema_migrations`: records successful application of this migration.

Board members, members, membership fees, consent, name visibility, sponsors, payment records and uploaded files are not modified.

## Compatibility and preservation

Every non-null historical year gets a Bangla Karneval edition, even if that year exists only in gallery or registration records. Existing record IDs and references are retained. Records with an unknown/null year remain unassigned rather than guessing an event; they should be resolved before edition IDs become mandatory in a later step.

For this transition, `event_config` and `active_event_year` remain the source used by the running application. Database triggers copy event setting edits into their legacy editions, assign edition IDs to new year-based records, and synchronize active-year changes. Foreign keys prevent deletion of a referenced edition and reject conflicting year/edition pairs.

Existing year constraints are retained for compatibility. New programmes must not be published or populated with live registrations manually yet: the existing APIs still filter by year. Later API/admin work must switch those queries to edition IDs before other programmes go live. The compatibility triggers and legacy constraints can then be retired deliberately. Programme management and themes are outside point 2.

## Applying the update

The running database has not been changed by the code edit. Before deploying, create a backup from the project directory:

```powershell
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/bk-before-programmes.dump'
docker compose cp db:/tmp/bk-before-programmes.dump ./bk-before-programmes.dump
```

Keep this backup private because it contains application data. Keep a separate backup of uploaded files as part of your normal deployment backup.

Then rebuild the backend:

```powershell
docker compose up -d --build backend
docker compose logs --tail=80 backend
```

After Hibernate prepares the existing schema and application settings initialize, the startup migration runs once in a database transaction. A database advisory lock prevents concurrent application instances from applying it together. A failure rolls back the migration and stops backend startup; it is not marked successful. A later startup skips an already completed migration.

The canonical SQL is `backend/src/main/resources/db/migrations/20260918_programme_foundation.sql`. Do not run a second hand-edited copy against production. Both fresh Docker databases and existing databases use the same startup path. No destructive down migration is supplied.

## Test scope

Automated PostgreSQL tests cover existing record preservation, missing-year handling, historical media, repeated migration, continued legacy writes, settings synchronization, multiple editions in one year, relationship constraints and transaction rollback. The existing membership and active-year integration tests use independent temporary schemas so they also exercise backend startup with the migration enabled.

Verification on 18 September 2026: the complete 38-test backend suite passed with PostgreSQL integration enabled. The migration test class then passed both preservation and the additional rollback test (2 tests). No production records were used or changed.
