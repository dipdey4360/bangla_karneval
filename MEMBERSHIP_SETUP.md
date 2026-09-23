# Board members and memberships

## Start the updated application

From the project folder in PowerShell, run:

```powershell
docker compose up -d --build backend
```

The existing production configuration uses Hibernate `ddl-auto: update`, so startup creates `board_members`, `members`, and the membership settings columns in `event_config`. Existing About Us content is retained. Board photographs are stored in the new persistent `board_uploads` Docker volume.

For an explicitly managed schema, apply the included migration to your existing database before rebuilding. This command uses the database container's configured username and database name:

```powershell
Get-Content .\database\migrations\20260916_membership.sql | docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
Get-Content .\database\migrations\20260916_partner_details.sql | docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
```

The migration is repeatable. Fresh databases also receive these additions through `database/init.sql`. Keep existing database credentials and volumes; do not use `docker compose down -v` to install this feature.

## Configure the public sections

1. Sign in at http://localhost:8084/admin.html using your existing administrator account.
2. Open **Board Members**. Choose each of the six slots, enter a name and designation, optionally upload a JPEG/PNG photograph (maximum 5 MB), and save. Empty slots display placeholders. The desktop layout has three cards per row and two rows; smaller screens use fewer columns.
3. Open **Memberships**. Set the benefits and your actual bank transfer / PayPal instructions. The default annual fees are EUR 25 for single and EUR 30 for couple membership. Save the settings.
4. Open http://localhost:8084/about_us.html and refresh the page to see the additions.

## Review applications

Applicants select single/couple membership, supply the requested details, declare that they have paid, and consent to data storage. They must choose whether their names may appear publicly; the choice applies to both partners for couples. Couple applications require the partner's full name, date of birth, address, phone and email as well. These fields appear and become required only for couples; the admin can review them with the application. Payment happens separately using the instructions you provide; selecting PayPal does not charge a PayPal account.

In **Memberships**, open **Review details** for a pending application. Check the bank/PayPal payment yourself, select accept or reject, and optionally enter a message for the applicant. Acceptance requires the payment-verification checkbox. Saving queues the decision email. Approved names appear on About Us only when the applicant chose public visibility, including both partners for a couple. Personal contact details and dates of birth remain available only through the administrator endpoints.

Refresh applications to check email delivery. If delivery fails, check your existing `MAIL_USERNAME` and `MAIL_PASSWORD` configuration, then use **Retry email**. Saving the same decision again does not send another email. The admin can read the visibility choice but cannot change it. Existing memberships retain their previous visibility; new applications must explicitly select an option.

Fee changes affect new applications; existing applications retain their original fee. This feature records applications and decisions; automatic yearly renewal, recurring charges, and membership expiry are not implemented.

To remove a membership, go to **Admin → Memberships**, find the record (select **All** if needed), click **Delete member**, and confirm the displayed names. This permanently deletes the application and its stored personal details from the application database, and removes the names from the public list on the next refresh. For couples, both partners belong to the same record and are deleted together. Pending and rejected applications can also be deleted. No deletion email is sent. Existing backups and previously sent emails are unaffected.

## Verification performed

- 14 automated tests passed, including an isolated PostgreSQL integration test covering approval, both public names, rejection, failed email delivery and retry, visibility, and saved settings.
- Security and request-validation tests cover public access and administrator-only access. Image tests cover invalid uploads and image re-encoding.
- New JavaScript files passed syntax checks. The application and approval browser flow was checked using a local preview with synthetic data, including the six-card desktop layout.
- The schema initialization and migration were checked against a disposable database, including repeated migration runs.
- Email sending was mocked during tests. Actual SMTP delivery must be verified with your configured mail account after deployment.

Active-year update: Membership settings now live independently in application_settings. On upgrade, the existing 2026 fees, benefits and payment instructions are copied once. Event-year changes do not change memberships or their public-name preferences. See ACTIVE_YEAR_SETUP.md.


## Membership IDs and event discounts

An approved membership receives a unique ID such as `BKM-00001`. A couple shares one ID; either partner verifies using their own full name. Approval emails include the ID and a confidentiality note, and are sent to both distinct couple email addresses. The admin membership details also show the ID.

Set **Memberships → Member event discount (%)** in the admin dashboard (0–100; default 0). Event registration includes **Are you already a member?** for the primary registrant and each additional participant. Selecting Yes reveals the ID field and Verify membership button. Changing the name or ID clears verification. Discounts apply only to that verified participant; children who already attend free remain free. Names match without differences in letter case or spacing. Approved status and verified membership donation are required. The server rechecks membership and calculates the discount on submission, rejecting changed totals instead of silently charging a different amount.

The new migration `20260923_membership_ids` runs through the existing backend startup migration system. It assigns IDs to already approved records without sending unsolicited emails. Existing members' IDs are available in admin details; existing sent confirmation emails are not changed. Pending applications receive an ID on their first approval. IDs survive later decisions and are never reused after deletion. Database sequences can leave gaps after rolled-back approvals. Numbers expand beyond five digits when necessary.

The partner address field has been removed from the membership application. Existing stored addresses are retained; new applications do not store a separate partner address.

Rebuild and restart the backend after pulling these changes (for the local Docker setup: `docker compose up -d --build backend`). A browser refresh alone does not activate the new verification endpoint. Configure the discount percentage before testing the discount. Eligibility requires approval, verified donation, and an unexpired membership.

Membership ID plus name is an eligibility check, not account authentication: sequential IDs are guessable. Keep IDs out of public member listings and avoid using them to authorize access to personal records.


## One-year validity and expiry reminders

On first board approval, the membership starts on the current Europe/Berlin date and expires one calendar year later. The expiry date is exclusive: a membership starting 2026-09-23 expires at the start of 2027-09-23. Leap-day approvals expire on February 28 the following year. Repeated approvals or reject/reapprove actions retain the original dates; they do not renew membership.

Admin **Memberships → Review details** shows the start date, expiry date, validity status, reminder due date, and separate reminder sent timestamps for the member and partner. Approved member cards also show expiry. Confirmation emails include validity dates. Expired members cannot verify for event discounts, and the server rechecks validity at registration submission.

The backend checks daily at **09:00 Europe/Berlin**. The reminder is due on `expiry minus one calendar month` (for example March 31 → February 28 in a non-leap year). Missed reminders catch up on the next daily run while membership remains valid. SMTP failures retry on later daily runs; successful recipients are not resent during normal retries. Couple members receive separate messages; a shared email address receives one message. Backend uptime and working SMTP are required. Reminder tracking records SMTP acceptance, not inbox delivery. A server crash after SMTP acceptance but before committing the sent timestamp can cause a duplicate on retry.

Startup migration `20260923_membership_expiry` fills dates for existing approved memberships using `reviewed_at`. It preserves any existing validity/reminder data. If a legacy record has no recorded approval date, dates remain unknown rather than inventing an approval date, and member discounts are unavailable until an administrator corrects the record. Pending applications have no dates until approval. The existing public names list remains governed by approval and visibility settings.

Rebuild the backend with `docker compose up -d --build backend` and refresh the admin page to activate the migration, scheduler, and updated details. No database reset is needed. This change does not add a renewal payment workflow or automatically extend expired memberships; reminders ask members to contact the board.
