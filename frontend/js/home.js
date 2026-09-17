/* ══════════════════════════════════════════════════════════════
   HOME PAGE — home.js
   ══════════════════════════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', () => {
    initBurgerMenu();
    loadEventConfig();
    loadEventCards();
    applyHeroButtonVisibility(); // ← added
});

/* ── Burger menu toggle ────────────────────────────────────── */
function initBurgerMenu() {
    const burger = document.getElementById('burger-btn');
    const links  = document.getElementById('hero-nav-links');
    if (!burger || !links) return;
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape' && links.classList.contains('open')) {
            links.classList.remove('open');
            burger.classList.remove('open');
            burger.setAttribute('aria-expanded', 'false');
            burger.focus();
        }
    });

    burger.addEventListener('click', (e) => {
        e.stopPropagation();
        const isOpen = links.classList.toggle('open');
        burger.classList.toggle('open', isOpen);
        burger.setAttribute('aria-expanded', isOpen);
    });

    links.querySelectorAll('.hero-nav-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            links.classList.remove('open');
            burger.classList.remove('open');
            burger.setAttribute('aria-expanded', false);
        });
    });

    document.addEventListener('click', (e) => {
        if (!burger.contains(e.target) && !links.contains(e.target)) {
            links.classList.remove('open');
            burger.classList.remove('open');
            burger.setAttribute('aria-expanded', false);
        }
    });
}

/* ── Show/hide hero buttons based on admin config ──────────── */
async function applyHeroButtonVisibility() {
    try {
        const res = await fetch('/api/config/button-status');
        if (!res.ok) return;
        const { registrationEnabled, performerEnabled } = await res.json();

        // Hero "Register Now" button
        const registerBtn = document.getElementById('hero-register-btn');
        if (registerBtn) registerBtn.style.display = registrationEnabled ? '' : 'none';

        // Hero nav "Performer" link
        const performerLink = document.getElementById('hero-performer-link');
        if (performerLink) performerLink.style.display = performerEnabled ? '' : 'none';

    } catch (e) {
        console.warn('Could not fetch button status:', e);
        // Both buttons remain visible on failure — safe default
    }
}

/* ── Load event config (date + social links) ───────────────── */
async function loadEventConfig() {
    try {
        const config = await getActiveEventConfig();

        const dateEl = document.getElementById('event-date');
        if (dateEl && config.eventDate) {
            dateEl.textContent = formatDate(config.eventDate);
        }

        const fb = document.getElementById('home-facebook');
        const ig = document.getElementById('home-instagram');
        if (fb && config.contactFacebook) {
            const url = safeWebUrl(config.contactFacebook);
            if (url) fb.href = url; else fb.removeAttribute('href');
        }
        if (ig && config.contactInstagram) {
            const url = safeWebUrl(config.contactInstagram);
            if (url) ig.href = url; else ig.removeAttribute('href');
        }

    } catch (e) {
        console.error('Failed to load event config:', e);
    }
}

/* ── Load event cards ──────────────────────────────────────── */
async function loadEventCards() {
    const grid = document.getElementById('event-cards');
    if (!grid) return;

    try {
        const config = await getActiveEventConfig();
        const events = await apiFetch(`/api/events/${config.eventYear}`);
        grid.innerHTML = '';

        if (!events || events.length === 0) {
            grid.innerHTML = '<p class="no-events">No events added yet.</p>';
            return;
        }

        events.forEach(ev => {
            const card = document.createElement('div');
            card.className = 'event-card';
            const icon = contentNode('div', null, 'event-icon');
            const image = contentNode('img');
            image.src = safeWebUrl(ev.iconUrl) || '/assets/images/default-event.png';
            image.alt = ev.title || '';
            image.addEventListener('error', () => { image.style.display = 'none'; });
            icon.append(image);
            card.append(icon, contentNode('h3', ev.title), contentNode('p', ev.description || ''));
            grid.appendChild(card);
        });

        grid.dataset.count = events.length;

    } catch (e) {
        console.error('Failed to load event cards:', e);
        grid.innerHTML = '<p class="no-events">Unable to load events.</p>';
    }
}

/* ── Format date helper ────────────────────────────────────── */
function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-GB', {
        day:   'numeric',
        month: 'long',
        year:  'numeric'
    });
}
