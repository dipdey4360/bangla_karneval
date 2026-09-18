# Managing programmes and events

Points 3–7 are implemented. The Docker application was rebuilt on 18 September 2026. Bangla Karneval 2026 remains active with its original poster.

## Create and publish an event

1. Open `http://localhost:8084/admin_dashboard.html`, sign in and refresh with **Ctrl+F5**.
2. Open **Config → Programmes & Events**.
3. Click **Create new event**. Choose Bangla Karneval, Eid, Puja, Bangla Noboborsho, BBQ or Game, then enter the year, title, description, date, venue and price.
4. Choose the theme and accent colour. Set whether attendee and performer registration are open. Enter the actual payment instructions that attendees should receive; old placeholder bank details are no longer displayed.
5. Save the event. Upload a PNG or JPEG poster if desired (maximum 5 MB and 16 million pixels). Otherwise, the programme's default artwork is used.
6. Click **Preview saved event**. Creating or previewing an event does not change the active public event.
7. Choose the event in the active-event selector and click **Activate selected event**. Confirm the switch. The public homepage, theme, registration details and event activities now use that event.

Only one event is active at a time. Saving changes to an already active event updates its public information immediately. Programme and year cannot be changed after creation; create another event for a different programme or year. Multiple events can share a year.

## Manage records

The dashboard's event selector chooses which event's registrations, performers, activities, statistics and exports you manage. Selecting an event for management does not activate it. Gallery uploads also select a specific event; public gallery tabs distinguish events even when their years match.

Existing registrations keep their saved event title, date, venue, price and payment instructions for subsequent emails. If the active event or its settings change while someone has a registration form open, they must reload before submitting.

## Shared organisation information

The organisation editor controls the shared About Us story and contact information. Board members, membership applications, membership consent/name visibility and approved members continue to belong to Bangla Karneval e.V. across all programmes. Sponsors remain shared.

## Deployment and verification

- Database and uploaded-file backups are in `.backups/programmes-20260918/`, excluded from Git. Treat these as private application data.
- Both startup migrations completed. Existing record counts and the selected year matched before and after deployment.
- The PostgreSQL-enabled backend suite passed all 41 tests. JavaScript syntax, rendering-security, gallery and membership checks passed.
- Disposable-data browser checks covered creation, preview, activation, poster validation/upload, the public registration page and phone-width layouts. No live test applications or emails were submitted.
- Actual SMTP delivery was not tested as part of this update.
- Programme posters persist in the new `programme_posters` Docker volume. Keep it with the database, gallery, board and sponsor volumes in future backups.

Historical records with an unknown year are retained without guessing their event. Migration scripts are tracked in `application_schema_migrations` and are skipped after successful application. Restoring a backup is an explicit recovery operation, not a normal restart step.
