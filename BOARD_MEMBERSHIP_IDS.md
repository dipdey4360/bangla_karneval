# Board membership IDs

Board positions 1–6 own BKM-00001–BKM-00006 respectively. These IDs are visible in the admin board cards. Edit the existing position to transfer its ID to a new occupant; clearing a position disables its ID. Verification requires the current occupant's full name and uses the admin-configured discount. A board ID is valid while the position is occupied and does not cover a partner.

Ordinary single/couple memberships continue to use their existing annual approval, expiry and reminder rules. New ordinary IDs begin at BKM-00007, or the next unused number if that range is already in use.

On backend startup, migration `20260924_board_membership_ids` reserves the six IDs and replaces conflicting ordinary membership IDs. It preserves all other membership details and records each replaced ID in `previous_membership_id`, shown in the admin membership review. Existing IDs above six remain unchanged. Old reserved IDs no longer verify ordinary memberships.

After deployment, review the affected membership records and communicate their replacement IDs. This migration does not send emails. Existing board occupants who also hold ordinary memberships retain those separate records, including any partner's annual eligibility.

Deploy the code with `docker compose up -d --build backend`. The migration runs automatically; do not rerun database initialization or remove database volumes.
