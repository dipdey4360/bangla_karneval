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
