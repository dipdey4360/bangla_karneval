let sortableInstance = null;
let sponsorsLoaded   = false;

// ── DO NOT auto-load on DOMContentLoaded ─────────────────────────────────────
// switchTab() in admin_dashboard.html calls loadSponsors() + loadPerformerStatus()
// only when the Sponsors tab is actually clicked.

// ── Performer Toggle ──────────────────────────────────────────────────────────
async function loadPerformerStatus() {
    try {
        const data = await apiFetch('/api/admin/config/performer-status');
        updatePerformerToggleUI(data.enabled);
    } catch (e) { console.error('Could not load performer status', e); }
}

async function togglePerformer() {
    try {
        const data = await apiFetchWithAuth(
            '/api/admin/config/performer-toggle',
            { method: 'PATCH' }
        );
        updatePerformerToggleUI(data.enabled);
    } catch (e) { console.error('Toggle failed', e); }
}

function updatePerformerToggleUI(enabled) {
    const btn  = document.getElementById('performer-toggle-btn');
    const text = document.getElementById('performer-status-text');
    if (!btn || !text) return;
    if (enabled) {
        btn.textContent = '🔴 Disable Performer Registration';
        btn.className   = 'btn btn-danger btn-sm';
        text.innerHTML  = '✅ Performer registration is currently <strong>ENABLED</strong>';
    } else {
        btn.textContent = '🟢 Enable Performer Registration';
        btn.className   = 'btn btn-success btn-sm';
        text.innerHTML  = '🔴 Performer registration is currently <strong>DISABLED</strong>';
    }
}

// ── Sponsors ──────────────────────────────────────────────────────────────────
async function loadSponsors() {
    try {
        const sponsors = await apiFetchWithAuth('/api/admin/sponsors');  // ← was apiFetch (missing auth!)
        renderSponsorList(sponsors || []);
    } catch (e) { console.error('Could not load sponsors', e); }
}

function isVisible(s) {
    // Fix Lombok naming: Boolean field "isVisible" → Jackson serializes as "visible"
    return s.visible ?? s.isVisible ?? true;
}

function renderSponsorList(sponsors) {
    const container = document.getElementById('sponsor-list');
    if (!container) return;

    if (!sponsors.length) {
        container.innerHTML = `
            <div style="text-align:center;padding:40px;color:var(--text-secondary)">
                <div style="font-size:2rem">⭐</div>
                <p style="margin-top:12px">No sponsors yet. Add your first sponsor!</p>
            </div>`;
        return;
    }

    container.replaceChildren();
    for (const s of sponsors) {
        const card = contentNode('div', null, 'sponsor-admin-card' + (isVisible(s) ? '' : ' sponsor-hidden'));
        card.dataset.id = s.id;
        const handle = contentNode('div', '⠿', 'drag-handle'); handle.title = 'Drag to reorder';
        const logo = contentNode('div', null, 'sponsor-admin-logo');
        const url = safeWebUrl(s.logoPath);
        if (url) { const image = contentNode('img'); image.src = url; image.alt = s.name || ''; logo.append(image); }
        else logo.append(contentNode('div', '🏢'));
        const info = contentNode('div', null, 'sponsor-admin-info');
        info.append(contentNode('div', s.name, 'sponsor-admin-name'));
        for (const [value,icon] of [[s.address,'📍'],[s.phone,'📞'],[s.websiteUrl,'🌐'],[s.description,'ℹ️']]) {
            if (value) info.append(contentNode('div', icon + ' ' + value, 'sponsor-admin-meta'));
        }
        const actions = contentNode('div', null, 'sponsor-admin-actions');
        for (const [title,icon,action] of [[isVisible(s)?'Hide':'Show',isVisible(s)?'fa-eye-slash':'fa-eye',()=>toggleSponsor(s.id)],['Edit','fa-pen-to-square',()=>editSponsor(s)],['Delete','fa-trash-can',()=>deleteSponsor(s.id,s.name)]]) {
            const button = contentNode('button', null, 'btn btn-sm ' + (title==='Delete'?'btn-danger':'btn-outline'));
            button.type = 'button'; button.title = title; button.append(contentNode('i', null, 'fa-regular '+icon));
            button.addEventListener('click',action); actions.append(button);
        }
        card.append(handle,logo,info,actions); container.append(card);
    }

    // Init drag-and-drop
    if (sortableInstance) sortableInstance.destroy();
    sortableInstance = Sortable.create(container, {
        handle:    '.drag-handle',
        animation: 150,
        onEnd:     saveOrder
    });
}

async function saveOrder() {
    const ids = [...document.querySelectorAll('.sponsor-admin-card')]
        .map(card => card.dataset.id);
    try {
        await apiFetchWithAuth('/api/admin/sponsors/reorder', {  // ← was apiFetch
            method:  'PUT',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(ids)
        });
    } catch (e) { console.error('Reorder failed', e); }
}

async function toggleSponsor(id) {
    try {
        await apiFetchWithAuth(`/api/admin/sponsors/${id}/toggle`, { method: 'PATCH' });
        sponsorsLoaded = false;
        loadSponsors();
    } catch (e) { console.error('Toggle failed', e); }
}

async function deleteSponsor(id, name) {
    if (!confirm(`Delete sponsor "${name}"? This cannot be undone.`)) return;
    try {
        await apiFetchWithAuth(`/api/admin/sponsors/${id}`, { method: 'DELETE' });
        sponsorsLoaded = false;
        loadSponsors();
    } catch (e) { console.error('Delete failed', e); }
}

// ── Modal ─────────────────────────────────────────────────────────────────────
function openSponsorModal() {
    document.getElementById('sponsor-modal-title').textContent  = 'Add Sponsor';
    document.getElementById('sponsor-id').value                 = '';
    document.getElementById('s-name').value                     = '';
    document.getElementById('s-address').value                  = '';
    document.getElementById('s-phone').value                    = '';
    document.getElementById('s-website').value                  = '';
    document.getElementById('s-description').value              = '';
    document.getElementById('s-logo').value                     = '';
    document.getElementById('s-logo-preview').style.display     = 'none';
    document.getElementById('sponsor-form').onsubmit            = submitSponsor;
    document.getElementById('sponsor-modal').style.display      = 'flex';  // only once
}

function editSponsor(s) {
    document.getElementById('sponsor-modal-title').textContent  = 'Edit Sponsor';
    document.getElementById('sponsor-id').value                 = s.id;
    document.getElementById('s-name').value                     = s.name        || '';
    document.getElementById('s-address').value                  = s.address     || '';
    document.getElementById('s-phone').value                    = s.phone       || '';
    document.getElementById('s-website').value                  = s.websiteUrl  || '';
    document.getElementById('s-description').value              = s.description || '';
    document.getElementById('s-logo').value                     = '';
    const preview = document.getElementById('s-logo-preview');
    if (s.logoPath) { preview.src = s.logoPath; preview.style.display = 'block'; }
    else            { preview.style.display = 'none'; }
    document.getElementById('sponsor-form').onsubmit            = submitSponsor;
    document.getElementById('sponsor-modal').style.display      = 'flex';  // only once
}

function closeSponsorModal() {
    document.getElementById('sponsor-modal').style.display = 'none';
}

function previewLogo(input) {
    const preview = document.getElementById('s-logo-preview');
    if (input.files && input.files[0]) {
        preview.src           = URL.createObjectURL(input.files[0]);
        preview.style.display = 'block';
    }
}

async function submitSponsor(e) {
    e.preventDefault();
    const btn      = document.querySelector('#sponsor-form [type="submit"]');
    const id       = document.getElementById('sponsor-id').value;
    const formData = new FormData();
    formData.append('name',        document.getElementById('s-name').value.trim());
    formData.append('address',     document.getElementById('s-address').value.trim());
    formData.append('phone',       document.getElementById('s-phone').value.trim());
    formData.append('websiteUrl',  document.getElementById('s-website').value.trim());
    formData.append('description', document.getElementById('s-description').value.trim());
    const logoFile = document.getElementById('s-logo').files[0];
    if (logoFile) formData.append('logo', logoFile);

    setLoading(btn, true);
    try {
        const url    = id ? `/api/admin/sponsors/${id}` : '/api/admin/sponsors';
        const method = id ? 'PUT' : 'POST';
        const res    = await fetch(url, {
            method,
            headers: { 'Authorization': `Bearer ${localStorage.getItem('adminToken')}` },
            body: formData
        });
        if (!res.ok) throw new Error(`Server error: ${res.status}`);
        setLoading(btn, false);  // ← ADD THIS LINE (was missing on success path)
        closeSponsorModal();
        sponsorsLoaded = false;
        await loadSponsors();
    } catch (err) {
        alert(`Save failed: ${err.message}`);
        setLoading(btn, false);
    }
}

