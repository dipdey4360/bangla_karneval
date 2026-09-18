const API_BASE = '';

// Match event names, not colour themes. Unknown events have no default artwork.
function eventPosterUrl(config) {
    const uploaded = safeWebUrl(config.posterPath);
    if (uploaded) return uploaded;
    const name = String(config.title || '').toLowerCase().trim()
        .replace(/[-_]+/g, ' ').replace(/\s+/g, ' ')
        .replace(/\s+\d{4}$/, '').trim();
    const posters = {
        'bangla karneval': 'bangla-karneval.svg',
        'puja': 'puja.svg', 'durga puja': 'puja.svg',
        'eid': 'Eid.svg', 'eid ul fitr': 'Eid.svg', 'eid ul adha': 'Eid.svg',
        'bangla noboborsho': 'Bangla_Noboborsho.svg'
    };
    return Object.hasOwn(posters, name) ? '/assets/images/programmes/' + posters[name] : null;
}

function showEventPoster(image, config) {
    if (!image) return;
    const url = eventPosterUrl(config);
    image.hidden = !url;
    image.style.display = url ? '' : 'none';
    if (url) image.src = url;
    else image.removeAttribute('src');
}

let activeEventConfigPromise;
function getActiveEventConfig(refresh = false) {
    if (refresh || !activeEventConfigPromise) {
        const previewId = location.pathname.endsWith('/event_preview.html') ? new URLSearchParams(location.search).get('edition') : null;
        activeEventConfigPromise = (previewId ? apiFetchWithAuth('/api/admin/editions/' + encodeURIComponent(previewId), {cache:'no-store'}) : apiFetch('/api/config/current', {cache:'no-store'})).catch(error => {
            activeEventConfigPromise = null; throw error;
        });
    }
    return activeEventConfigPromise;
}
async function updateEventYearLabels() {
    try {
        const config = await getActiveEventConfig();
        document.querySelectorAll('[data-event-year]').forEach(node => node.textContent = config.eventYear);
        const base = document.documentElement.dataset.baseTitle || document.title;
        document.documentElement.dataset.baseTitle = base;
        document.title = location.pathname.endsWith('index.html') || location.pathname === '/' ? config.title + ' | Bangla Karneval e.V.' : base.replace('Bangla Karneval','Bangla Karneval e.V.');
        document.querySelectorAll('[data-event-title]').forEach(node => node.textContent = config.title || ('Bangla Karneval '+config.eventYear));
        const themes=['bangla-karneval','eid','puja','bangla-noboborsho','bbq','game'];
        document.documentElement.dataset.theme=themes.includes(config.themeKey)?config.themeKey:'bangla-karneval';
        if(/^#[0-9a-fA-F]{6}$/.test(config.accentColor||'')) document.documentElement.style.setProperty('--event-accent',config.accentColor);
    } catch (error) { console.warn('Active event year could not be loaded', error); }
}
document.addEventListener('DOMContentLoaded', updateEventYearLabels);

// ── Public fetch (no auth) ────────────────────────────────────────────────────
async function apiFetch(url, options = {}) {
    const response = await fetch(API_BASE + url, options);
    if (!response.ok) {
        const err = await response.json().catch(() => ({ message: response.statusText }));
        throw new Error(err.message || 'Request failed');
    }
    // Handle empty body responses (204 No Content, 205, etc.)
    const contentType = response.headers.get('content-type');
    if (response.status === 204 || !contentType || !contentType.includes('application/json')) {
        return null;
    }
    return response.json();
}

// ── Authenticated fetch (admin JWT) ───────────────────────────────────────────
async function apiFetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('adminToken');

    // No token at all → back to login
    if (!token) {
        window.location.replace('/admin.html');
        return;
    }

    const response = await fetch(API_BASE + url, {
        ...options,
        headers: {
            ...options.headers,
            'Authorization': `Bearer ${token}`
        }
    });

    // Token expired or no permission → clear token and redirect
    if (response.status === 401 || response.status === 403) {
        localStorage.removeItem('adminToken');
        window.location.replace('/admin.html');
        return;
    }

    if (!response.ok) {
        const err = await response.json().catch(() => ({ message: response.statusText }));
        throw new Error(err.message || 'Request failed');
    }

    // Handle empty body responses (DELETE → 204, etc.)
    const contentType = response.headers.get('content-type');
    if (response.status === 204 || !contentType || !contentType.includes('application/json')) {
        return null;
    }
    return response.json();
}

// ── Utility helpers ───────────────────────────────────────────────────────────
function formatDate(dateStr) {
    if (!dateStr) return 'TBA';
    return new Date(dateStr).toLocaleDateString('en-GB', {
        day: 'numeric', month: 'long', year: 'numeric'
    });
}

function formatCurrency(amount) {
    return `€${parseFloat(amount || 0).toFixed(2)}`;
}

function formatDateTime(dateStr) {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('en-GB', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

function showAlert(containerId, message, type = 'success') {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.innerHTML = `<div class="alert alert-${type}">${message}</div>`;
    el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    if (type === 'success') setTimeout(() => el.innerHTML = '', 6000);
}

function validateEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function setLoading(btnEl, loading) {
    if (!btnEl) return;
    if (loading) {
        btnEl.dataset.original = btnEl.innerHTML;
        btnEl.innerHTML        = '<span class="spinner"></span>';
        btnEl.disabled         = true;
    } else {
        btnEl.innerHTML = btnEl.dataset.original || 'Submit';
        btnEl.disabled  = false;
    }
}

function escapeHtml(str) {
    return String(str ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
}

// Use text nodes for API content and allow only web URLs for links/images.
function contentNode(tag, text, className) {
    const node = document.createElement(tag);
    if (text != null) node.textContent = String(text);
    if (className) node.className = className;
    return node;
}
function safeWebUrl(value) {
    if (typeof value !== 'string' || !value.trim()) return '';
    try {
        const url = new URL(value, window.location.origin);
        return ['http:', 'https:'].includes(url.protocol) ? url.href : '';
    } catch { return ''; }
}

// ── Page transition — professional loading experience ─────────────────────────
(function () {
    const overlay = document.createElement('div');
    overlay.id = 'page-transition-overlay';
    overlay.innerHTML = `
        <div class="pt-content">
            <div class="pt-logo">Bangla Karneval</div>
            <div class="pt-bar-wrap"><div class="pt-bar"></div></div>
        </div>
    `;
    overlay.style.cssText = `
        position:       fixed;
        inset:          0;
        background:     linear-gradient(135deg, rgba(45,22,8,0.98) 0%, rgba(28,12,4,0.98) 100%);
        z-index:        99999;
        opacity:        0;
        pointer-events: none;
        transition:     opacity 0.2s ease;
        display:        flex;
        align-items:    center;
        justify-content:center;
    `;

    document.addEventListener('DOMContentLoaded', () => {
        document.body.appendChild(overlay);
    });

    document.addEventListener('click', (e) => {
        const link = e.target.closest('a[href]');
        if (!link) return;
        const href = link.getAttribute('href');
        if (!href ||
            href.startsWith('#') ||
            href.startsWith('http') ||
            href.startsWith('mailto') ||
            href.startsWith('tel') ||
            href.startsWith('javascript') ||
            link.target === '_blank') return;

        e.preventDefault();
        overlay.style.pointerEvents = 'all';
        overlay.style.opacity       = '1';

        // Animate the progress bar
        const bar = overlay.querySelector('.pt-bar');
        if (bar) {
            bar.style.transition = 'none';
            bar.style.width      = '0%';
            requestAnimationFrame(() => {
                requestAnimationFrame(() => {
                    bar.style.transition = 'width 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
                    bar.style.width      = '85%';
                });
            });
        }

        setTimeout(() => {
            if (bar) bar.style.width = '100%';
            setTimeout(() => { window.location.href = href; }, 100);
        }, 380);
    });

    window.addEventListener('pageshow', () => {
        const ol = document.getElementById('page-transition-overlay');
        if (!ol) return;
        ol.style.opacity       = '0';
        ol.style.pointerEvents = 'none';
        const bar = ol.querySelector('.pt-bar');
        if (bar) bar.style.width = '0%';
    });
})();

