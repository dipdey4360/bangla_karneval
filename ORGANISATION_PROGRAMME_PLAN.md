# Point 1 — Organisation and programme boundaries

Status: point 1 scope and ownership defined from the existing code. Point 2 database implementation is documented in PROGRAMME_DATABASE_SETUP.md. Public-page and programme-management changes remain deferred.

## Organisation

The umbrella organisation is **Bangla Karneval e.V.** Its information remains the same when an administrator changes the active event.

- Organisation name and identity in shared navigation and footer.
- About Us organisation introduction and Our Story.
- Board members, their photographs and designations.
- Membership applications, approvals, personal details and public-name preferences.
- Public members list, membership benefits, annual fees and membership payment instructions.

Board members and memberships are already independent of event years. Preserve this behaviour, including both partners for couple memberships and member-controlled name visibility.

## Programme and event occurrence

A programme is a type of activity: Bangla Karneval, Eid, Puja, Bangla Noboborsho, BBQ or Game. An event occurrence is one particular edition of a programme, with its own date and settings. It must eventually have an identity beyond its year so two programmes, or two BBQs, can happen in the same year.

- Event title, year, date and venue.
- Homepage poster, theme colours, tagline and event description.
- What to Expect activities.
- Admission pricing and event registration availability.
- Performer registration availability.
- Attendee and performer registrations, payment status, statistics and exports.
- Gallery content associated with the event.
- Event names and details in registration and performer emails.

Bangla Karneval remains a programme name as well as part of the organisation name. Preserve the existing event's poster and design as its future theme. A Puja theme must not rename the organisation or replace its board or membership information.

## Page behaviour

| Area | Organisation content | Active-event content |
| --- | --- | --- |
| Home | Organiser identity: Bangla Karneval e.V. | Poster, title, introduction, date, activities and registration link |
| About Us | Organisation story, board, members and membership application | A separately labelled current-event date/venue block |
| Registration | Organiser identity | Event title, date, fees and availability |
| Performer registration | Organiser identity | Event title and performer availability |
| Gallery | Shared site navigation | Media belonging to the selected event |
| Admin | Shared organisation, board and membership controls | Event settings and event-specific records |

## Existing areas that need care in later implementation

- `EventConfig.aboutText` currently feeds Our Story. Preserve existing content and deliberately assign it during the later migration; do not discard or silently overwrite it with a generic introduction.
- `ApplicationSettings` already owns membership settings. Event activation must continue to leave them unchanged.
- Contact fields are currently stored per event year, and the contact page uses the event venue. Do not silently reinterpret an event venue as the association's address. Keep existing contact behaviour until organisation contact fields and any event-specific overrides are explicitly implemented.
- Sponsors currently have no event-year association. Keep them shared for this scope; any programme-specific sponsorship feature needs a separate decision.
- Existing year-only links and record associations are unchanged in point 1.

## Deferred work

Point 2 covers database structure, migration and preservation of existing event records. Subsequent points cover programme administration, activation, themes, event-specific records and verification. Do not start those changes without the user's instruction.
