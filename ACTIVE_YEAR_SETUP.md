# Active event year

## Install the update

From the project folder:

```powershell
docker compose up -d --build backend
```

Refresh the admin page with **Ctrl + F5**. The existing Hibernate schema update creates `application_settings`. On the first startup, the application keeps **2026** active and copies the existing 2026 membership fees, benefits, and payment instructions into the independent settings record. Later restarts preserve your selected active year and membership settings.

Existing registrations, performers, event cards, board members, gallery files, and membership records are not rewritten. The old membership columns in `event_config` remain for compatibility; the Memberships section now uses `application_settings`.

For installations that manage the schema explicitly instead of Hibernate update, run `database/migrations/20260917_active_event_year.sql` before starting the updated backend. The SQL can be repeated without resetting the active year or overwriting membership settings.

## Prepare and activate an event

1. Open **Admin → Config** and use **Event Years → Add Year** if the year does not exist yet.
2. Select that year in **View / manage event year** at the top of the dashboard.
3. Fill in its price, date, venue, description, and contact details, then click **Save Configuration**. This does not activate the year.
4. Add event cards for that year in the Performers tab's event-management section. Gallery uploads retain their own year selector.
5. In **Config → Active Event Year**, choose the year and click **Activate selected year**. Wait for the confirmation.

The active year determines public event details, titles, prices, event cards, gallery highlights, and new attendee/performer applications. Browser forms retain the year they originally loaded. If the active year changes before submission, the backend rejects the old form with a refresh message instead of silently registering the applicant for another year.

## Historical records and memberships

**View / manage event year** filters registrations, statistics, CSV exports, performer applications, event cards, and configuration. Selecting a historical year does not change the public active year. Event-related emails use the record's original year, including when an older registration is updated.

Board members and memberships apply across event years. Changing the active event does not change membership fees, benefits, payment instructions, approvals, or applicant-selected name visibility. The public button-visibility controls apply to the active event year.

## Verification

All 32 backend tests passed, including isolated PostgreSQL tests of activation, stale-form rejection, year-specific registration prices and records, historical filters, and unchanged membership settings. No actual emails were sent by tests. SQL upgrade checks verified copying existing membership settings and preserving them on repeated migration runs. Browser checks in an isolated preview verified preparing an upcoming year, activation confirmation, and the updated public year and price. JavaScript syntax and existing gallery/security checks passed.
