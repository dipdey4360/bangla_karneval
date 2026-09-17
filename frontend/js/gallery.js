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

        // Load every gallery year, including the current event year
        await loadGalleryYearTabs();

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

/* ── Load year tabs including the current year ────────── */
async function loadGalleryYearTabs() {
    const tabsContainer = document.getElementById('year-tabs');
    const grid          = document.getElementById('year-gallery-grid');

    try {
        // Returns years that have actual images in gallery_items
        const allYears     = await apiFetch('/api/gallery/years');
        const galleryYears = [...new Set([Number(currentEventYear), ...allYears.map(Number)])]
            .filter(Number.isInteger).sort((a,b) => b-a);

        if (galleryYears.length === 0) {
            tabsContainer.innerHTML = '<p class="gallery-empty" style="font-size:14px">No gallery years available.</p>';
            grid.innerHTML          = '';
            return;
        }

        // Build tabs
        tabsContainer.replaceChildren();
        galleryYears.forEach(year => {
            const tab = contentNode('button', year, 'year-tab');
            tab.type = 'button'; tab.dataset.year = year;
            tabsContainer.append(tab);
        });

        // Attach click listeners
        tabsContainer.querySelectorAll('.year-tab').forEach(tab => {
            tab.addEventListener('click', () =>
                loadYearGallery(parseInt(tab.dataset.year))
            );
        });

        // Auto-load the current event year
        await loadYearGallery(Number(currentEventYear));

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
            `No photos or videos uploaded for ${year} yet.`);
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
    container.replaceChildren();
    items.forEach(item => {
        const card = contentNode('button', null, 'gallery-item'); card.type = 'button';
        const video = item.mediaType === 'VIDEO';
        card.setAttribute('aria-label', (video ? 'Play video: ' : 'View image: ') + (item.caption || 'Gallery item'));
        const media = contentNode(video ? 'video' : 'img');
        media.src = safeWebUrl(item.url);
        if (video) { media.muted = true; media.preload = 'metadata'; media.playsInline = true; }
        else { media.alt = item.caption || ''; media.loading = 'lazy'; }
        const overlay = contentNode('div', null, 'gallery-item-overlay');
        overlay.append(contentNode('span', (video ? '▶ ' : '') + (item.caption || ''), 'gallery-item-caption'));
        card.append(media, overlay); card.addEventListener('click', () => openLightbox(item));
        container.append(card);
    });
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
    const video = document.getElementById('lightbox-video');
    video.pause(); video.removeAttribute('src'); video.load();
    const isVideo = item.mediaType === 'VIDEO';
    img.hidden = isVideo; video.hidden = !isVideo;
    img.removeAttribute('src');
    if (isVideo) video.src = safeWebUrl(item.url);
    else { img.src = safeWebUrl(item.url); img.alt = item.caption || 'Gallery image'; }
    if (cap) cap.textContent = item.caption || '';
    lb.classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeLightbox() {
    const video = document.getElementById('lightbox-video');
    if (video) { video.pause(); video.removeAttribute('src'); video.load(); }
    document.getElementById('lightbox')?.classList.remove('open');
    document.body.style.overflow = '';
}
