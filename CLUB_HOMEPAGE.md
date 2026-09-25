# Club homepage and public theme

The public homepage introduces Bangla Karneval e.V. with **A little Bangla. A shared home.** The shared cream, burgundy and serif theme covers Home, About Us, Gallery, Contact, Registration, Performer Registration and the admin's event preview. Administrative workflows remain in the existing dashboard.

The general line **Celebrating Bengali culture, heritage & community** appears above the carousel. Below each poster, the event's own tagline, date and venue are displayed. The current event opens in the centre, with subdued neighbouring posters, arrow buttons, event selectors and touch swiping. Reduced-motion preferences are respected. Missing dates and venues are shown as unannounced. Only the active edition can offer registration, and only while its registration setting is enabled. Preview mode cannot open registration.

## Admin controls

Under event configuration, select an event and edit **Event tagline (shown below the homepage poster)**, date, venue and description. Upload a poster using the existing poster form. Use **Activate selected event** to choose the homepage's current event. The What to Expect section follows the selected event's activities. Organisation social links are shared in the footer.

Migration `20260924_club_homepage` creates missing Eid, Puja, Noboborsho, Bangla Karneval and BBQ editions for the configured active year. Existing editions, custom taglines and uploaded posters are preserved. The migration fills only blank or old generic taglines. Newly created editions have no invented dates or venues, and registration is disabled until configured by an administrator. The public catalogue includes all editions; event details become visible in that catalogue when saved. No changes are made to existing registrations or memberships.

The `/api/editions/home` endpoint returns a small public projection: edition ID, programme, title, tagline, date, venue, year, poster and registration availability. Payment instructions, private contact fields and version metadata are excluded. The existing gallery edition endpoint is unchanged.

## Activate and verify

Rebuild the backend with `docker compose up -d --build backend`, then refresh the website. The startup migration runs automatically. Do not reset the database. Confirm the active event and enter dates/venues for the new editions in the dashboard.

Validation: backend tests include public catalogue projection and repeatable migration; `tests/club-homepage-browser.cjs` checks all public pages at 1280, 768, 390 and 320 px using isolated API fixtures, membership verification, navigation, carousel controls and admin preview. It requires Playwright and an installed browser. Set `BK_BROWSER_CHANNEL=chrome` to use Chrome; optional `BK_PREVIEW_DIR` writes screenshots. Existing gallery, registration discount, admin membership and safe-rendering checks remain available.

## BBQ artwork

Asset: `frontend/assets/images/programmes/bbq.png`. Created with the built-in image-generation tool using the existing Eid poster as a style reference and the club logo as a brand reference. The existing Eid, Puja, Noboborsho and Bangla Karneval images are reused.

Generation prompt:

> Create a landscape 3:2 BBQ event poster for Bangla Karneval e.V. Image 1 is a style reference only: match its elegant ivory background, thin gold corner ornament, generous whitespace and refined serif lettering. Image 2 is the exact club logo to faithfully include above the title on the right, preserving lettering and colors. Replace Eid symbolism with a tasteful hand illustrated charcoal barbecue grill, vegetables and foliage on the left in muted terracotta, gold and deep olive. Text exactly: 'BBQ' and below it 'Bangla Karneval e.V.'. No date, venue or other text. Polished, welcoming nonprofit community event artwork. Keep all content within wide margins.
