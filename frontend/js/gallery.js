/* ══════════════════════════════════════════════════════════════
   GALLERY — gallery.js
   ══════════════════════════════════════════════════════════════ */

let currentEventYear = null;

document.addEventListener('DOMContentLoaded', async () => {
    await initGallery();
    setupLightbox();
});

/* ── Init — load config first to get current year ──────────── */
async function initGallery() {
    try {
        // Get current event year from config
        const config = await apiFetch('/api/config/current');
        currentEventYear = config.eventYear;

        // Update highlights section title
        const titleEl = document.getElementById('highlights-title');
        if (titleEl) titleEl.textContent = `${currentEventYear} Event Highlights`;

        // Load highlights for current year
        await loadHighlights(currentEventYear);

        // Load previous years tabs from gallery_items
        await loadPreviousYearTabs();

    } catch (e) {
        console.error('Gallery init failed:', e);
        document.getElementById('highlights-grid').innerHTML =
            '<p class="gallery-empty">Failed to load gallery.</p>';
    }
}

/* ── Load highlights for current year ─────────────────────── */
async function loadHighlights(year) {
    const grid = document.getElementById('highlights-grid');
    grid.innerHTML = `
        <div class="gallery-item skeleton"></div>
        <div class="gallery-item skeleton"></div>
        <div class="gallery-item skeleton"></div>
        <div class="gallery-item skeleton"></div>`;
    try {
        const items = await apiFetch(`/api/gallery/${year}?highlight=true`);
        renderGallery(items, 'highlights-grid',
            `No highlights added for ${year} yet.`);
    } catch (e) {
        console.error('Highlights load failed:', e);
        grid.innerHTML = '<p class="gallery-empty">Failed to load highlights.</p>';
    }
}

/* ── Load previous year tabs (exclude current year) ────────── */
async function loadPreviousYearTabs() {
    const tabsContainer = document.getElementById('year-tabs');
    const grid          = document.getElementById('year-gallery-grid');

    try {
        // Returns years that have actual images in gallery_items
        const allYears     = await apiFetch('/api/gallery/years');
        const previousYears = allYears.filter(y => y !== currentEventYear);

        if (previousYears.length === 0) {
            tabsContainer.innerHTML = '<p class="gallery-empty" style="font-size:14px">No previous years uploaded yet.</p>';
            grid.innerHTML          = '';
            return;
        }

        // Build tabs
        tabsContainer.innerHTML = previousYears.map((year, index) => `
            <div class="year-tab ${index === 0 ? 'active' : ''}"
                 data-year="${year}">${year}</div>
        `).join('');

        // Attach click listeners
        tabsContainer.querySelectorAll('.year-tab').forEach(tab => {
            tab.addEventListener('click', () =>
                loadYearGallery(parseInt(tab.dataset.year))
            );
        });

        // Auto-load first previous year
        await loadYearGallery(previousYears[0]);

    } catch (e) {
        console.error('Previous years load failed:', e);
        tabsContainer.innerHTML = '<p class="gallery-empty">Failed to load years.</p>';
    }
}

/* ── Load gallery for selected year ────────────────────────── */
async function loadYearGallery(year) {
    // Update active tab
    document.querySelectorAll('.year-tab').forEach(t =>
        t.classList.toggle('active', parseInt(t.dataset.year) === year)
    );

    const grid = document.getElementById('year-gallery-grid');
    if (!grid) return;

    grid.innerHTML = `
        <div class="gallery-item skeleton"></div>
        <div class="gallery-item skeleton"></div>
        <div class="gallery-item skeleton"></div>
        <div class="gallery-item skeleton"></div>`;

    try {
        const items = await apiFetch(`/api/gallery/${year}`);
        renderGallery(items, 'year-gallery-grid',
            `No images uploaded for ${year} yet.`);
    } catch (e) {
        console.error(`Gallery load failed for ${year}:`, e);
        grid.innerHTML = '<p class="gallery-empty">Failed to load gallery.</p>';
    }
}

/* ── Render gallery grid ────────────────────────────────────── */
function renderGallery(items, containerId, emptyMessage = 'No images yet.') {
    const container = document.getElementById(containerId);
    if (!container) return;
    if (!items || items.length === 0) {
        container.innerHTML = `<p class="gallery-empty">${emptyMessage}</p>`;
        return;
    }
    container.innerHTML = items.map(item => `
        <div class="gallery-item"
             onclick="openLightbox(${JSON.stringify(item).replace(/"/g, '&quot;')})">
            <img src="${escapeHtml(item.url)}"
                 alt="${escapeHtml(item.caption || '')}"
                 loading="lazy">
            <div class="gallery-item-overlay">
                <span class="gallery-item-caption">
                    ${escapeHtml(item.caption || '')}
                </span>
            </div>
        </div>`).join('');
}

/* ── Lightbox ───────────────────────────────────────────────── */
function setupLightbox() {
    const lb = document.getElementById('lightbox');
    if (!lb) return;
    lb.addEventListener('click', (e) => { if (e.target === lb) closeLightbox(); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeLightbox(); });
}

function openLightbox(item) {
    const lb  = document.getElementById('lightbox');
    const img = document.getElementById('lightbox-image');
    const cap = document.getElementById('lightbox-caption');
    if (!lb || !img) return;
    img.src = item.url;
    if (cap) cap.textContent = item.caption || '';
    lb.classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeLightbox() {
    document.getElementById('lightbox')?.classList.remove('open');
    document.body.style.overflow = '';
}
