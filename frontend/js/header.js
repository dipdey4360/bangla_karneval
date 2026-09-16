/* ══════════════════════════════════════════════════════════════
   HEADER — header.js
   ══════════════════════════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', async () => {
    const container = document.getElementById('header-container');
    if (!container) return;
    try {
        const res = await fetch('/components/header.html');
        container.innerHTML = await res.text();
        initNavigation();
        initHamburger();
        await applyButtonVisibility();  // ← replaces checkPerformerStatus()
    } catch (e) {
        console.error('Failed to load header:', e);
    }
});

function initNavigation() {
    const currentPath = window.location.pathname.split('/').pop() || 'index.html';
    const links = document.querySelectorAll('.main-nav a[data-page]');
    const pageMap = {
        'index.html':                  'home',
        '':                            'home',
        'about_us.html':               'about',
        'gallery.html':                'gallery',
        'contact.html':                'contact',
        'registration.html':           'register',
        'performer_registration.html': 'performer'
    };
    const activePage = pageMap[currentPath];
    links.forEach(link => {
        if (link.dataset.page === activePage) link.classList.add('active');
    });
}

function initHamburger() {
    const hamburger = document.getElementById('hamburger');
    const nav       = document.getElementById('main-nav');
    if (!hamburger || !nav) return;

    hamburger.addEventListener('click', (e) => {
        e.stopPropagation();
        const isOpen = nav.classList.toggle('open');
        hamburger.classList.toggle('open', isOpen);
        hamburger.setAttribute('aria-expanded', isOpen);
    });

    // Close on nav link click
    nav.querySelectorAll('a').forEach(link => {
        link.addEventListener('click', () => {
            nav.classList.remove('open');
            hamburger.classList.remove('open');
            hamburger.setAttribute('aria-expanded', false);
        });
    });

    // Close on outside click
    document.addEventListener('click', (e) => {
        if (!hamburger.contains(e.target) && !nav.contains(e.target)) {
            nav.classList.remove('open');
            hamburger.classList.remove('open');
            hamburger.setAttribute('aria-expanded', false);
        }
    });
}

/* ── Apply both button visibility states ───────────────────────
   Single API call replaces the old checkPerformerStatus().
   Uses the public /api/config/button-status endpoint.
   Defaults to visible if the request fails (safe fallback).
─────────────────────────────────────────────────────────────── */
async function applyButtonVisibility() {
    try {
        const res = await fetch('/api/config/button-status');
        if (!res.ok) return;
        const { registrationEnabled, performerEnabled } = await res.json();

        // ── Performer nav link ──────────────────────────────
        setNavItemVisible('performer', performerEnabled);

        // ── Register Now nav link ───────────────────────────
        setNavItemVisible('register', registrationEnabled);

    } catch (e) {
        console.warn('Could not fetch button status:', e);
        // Both buttons remain visible on failure — safe default
    }
}

/* ── Helper: show/hide a nav <li> by data-page value ────────── */
function setNavItemVisible(page, visible) {
    const link = document.querySelector(`.main-nav a[data-page="${page}"]`);
    if (!link) return;
    const li = link.closest('li') ?? link.parentElement;
    if (li) li.style.display = visible ? '' : 'none';
    else    link.style.display = visible ? '' : 'none';
}
