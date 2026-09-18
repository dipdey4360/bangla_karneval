let allRegistrations = [];
let allEvents = [];

document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('adminToken');
    if (!token) { window.location.href = 'admin.html'; return; }

    document.getElementById('admin-name-display').textContent =
        localStorage.getItem('adminName') || 'Admin';
    document.getElementById('logout-btn')?.addEventListener('click', logout);

    // Only load what the default active tab (Registrations) needs
    await ensureAdminYears();
    await Promise.all([loadDashboardStats(), loadRegistrations()]);
    setupSearch();
    setupFilter();
    if (typeof requestIdleCallback !== 'undefined') {
        requestIdleCallback(() => {
            if (!navigator.connection?.saveData) loadPerformers();
        });
    }
    loadUnreadCount();
    setInterval(loadUnreadCount, 60000);

    // ── Close sidebar on nav link click (mobile) ──────────────
    document.querySelectorAll('.sidebar-nav a').forEach(link => {
        link.addEventListener('click', () => {
            if (window.innerWidth <= 1024) {
                document.getElementById('sidebar').classList.remove('open');
                document.getElementById('sidebar-overlay').classList.remove('active');
            }
        });
    });
});



// ── Stats ─────────────────────────────────────────────────────────────────────
async function loadDashboardStats() {
    try {
        const selected=adminSelectedEdition;
        const stats = await apiFetchWithAuth('/api/admin/dashboard/stats' + adminYearQuery());
        if(selected!==adminSelectedEdition)return;
        document.getElementById('stat-total').textContent   = stats.totalRegistrations || 0;
        document.getElementById('stat-paid').textContent    = stats.totalPaid          || 0;
        document.getElementById('stat-unpaid').textContent  = (stats.totalUnpaid || 0) + (stats.totalOverdue || 0);
        document.getElementById('stat-revenue').textContent = formatCurrency(stats.totalRevenue);
        renderAgeGroups(stats);
    } catch (e) { console.error('Stats load failed:', e); }
}

function renderAgeGroups(stats) {
    const el = document.getElementById('age-group-table');
    if (!el) return;
    const total = (stats.childrenCount + stats.adultsCount + stats.seniorsCount) || 1;
    const row = (label, count) => {
        const pct = Math.round((count / total) * 100);
        return `<tr>
            <td>${label}</td>
            <td><strong>${count}</strong></td>
            <td style="width:120px"><span class="age-bar" style="width:${pct}%"></span></td>
            <td style="color:var(--text-secondary);font-size:13px">${pct}%</td>
        </tr>`;
    };
    el.innerHTML = `
        <table class="age-group-table">
            ${row('👶 Children (<18)', stats.childrenCount || 0)}
            ${row('🧑 Adults (18-59)', stats.adultsCount   || 0)}
            ${row('👴 Seniors (60+)',  stats.seniorsCount  || 0)}
        </table>`;
}

// ── Registrations ─────────────────────────────────────────────────────────────
function showTableSkeleton(tbodyId, cols = 9, rows = 5) {
    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;
    tbody.innerHTML = Array(rows).fill(`
        <tr>${Array(cols).fill(`
            <td><div class="skeleton-row"></div></td>
        `).join('')}</tr>
    `).join('');
}


async function loadRegistrations(status = 'ALL', search = '') {
    showTableSkeleton('registrations-tbody', 9, 5);
    try {
        const selected=adminSelectedEdition;
        const params = new URLSearchParams();
        if (adminSelectedEdition !== null) params.set("eventEditionId", adminSelectedEdition);
        if (status !== 'ALL') params.set('status', status);
        if (search) params.set('search', search);
        const loaded = await apiFetchWithAuth(`/api/admin/registrations?${params}`);
        if(selected!==adminSelectedEdition)return;
        allRegistrations=loaded || [];
        renderRegistrationTable(allRegistrations);
    } catch (e) { console.error('Registrations load failed:', e); }
}

function renderRegistrationTable(registrations) {
    const tbody = document.getElementById('registrations-tbody');
    if (!tbody) return;
    if (!registrations || !registrations.length) {
        tbody.innerHTML = `<tr><td colspan="9"
            style="text-align:center;padding:32px;color:var(--text-secondary)">
            No registrations found.</td></tr>`;
        return;
    }
    tbody.innerHTML = registrations.map(r => `
        <tr>
            <td><strong>${escapeHtml(r.referenceCode || '')}</strong></td>
            <td>${escapeHtml(r.primaryName || '')}</td>
            <td style="font-size:13px">${escapeHtml(r.email || '')}</td>
            <td>${escapeHtml(r.phone || '-')}</td>
            <td style="text-align:center">${r.participantCount || 1}</td>
            <td><strong>${formatCurrency(r.calculatedAmount)}</strong></td>
            <td><span class="badge badge-${(r.paymentStatus || '').toLowerCase()}">
                ${r.paymentStatus || ''}</span></td>
            <td style="font-size:12px">${formatDateTime(r.registeredAt)}</td>
            <td>
                <div class="actions">
                    <button class="btn btn-sm btn-outline" onclick="openEditModal('${r.id}')" title="Edit"><i class="fa-regular fa-pen-to-square"></i></button>
                    <button class="btn btn-sm btn-danger" onclick="deleteRegistration('${r.id}', this)" title="Delete"><i class="fa-regular fa-trash-can"></i></button>
                </div>
            </td>
        </tr>`).join('');
}

function setupSearch() {
    document.getElementById('search-input')?.addEventListener('input', (e) => {
        const term   = e.target.value.toLowerCase();
        const status = document.getElementById('status-filter')?.value || 'ALL';
        const filtered = allRegistrations.filter(r =>
            (r.primaryName || '').toLowerCase().includes(term) &&
            (status === 'ALL' || r.paymentStatus === status)
        );
        renderRegistrationTable(filtered);
    });
}

function setupFilter() {
    document.getElementById('status-filter')?.addEventListener('change', (e) => {
        const search = document.getElementById('search-input')?.value || '';
        loadRegistrations(e.target.value, search);
    });
}

// ── Edit Registration Modal ───────────────────────────────────────────────────
function openEditModal(id) {
    const reg = allRegistrations.find(r => String(r.id) === String(id));
    if (!reg) return;
    document.getElementById('edit-id').value            = reg.id;
    document.getElementById('edit-name').textContent    = reg.primaryName;
    document.getElementById('edit-phone').value         = reg.phone         || '';
    document.getElementById('edit-email').value         = reg.email         || '';
    document.getElementById('edit-address').value       = reg.address       || '';
    document.getElementById('edit-status').value        = reg.paymentStatus || 'PENDING';
    document.getElementById('edit-modal').style.display = 'flex';
}

document.getElementById('edit-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = e.target.querySelector('[type="submit"]');
    setLoading(btn, true);
    const id = document.getElementById('edit-id').value;
    try {
        await apiFetchWithAuth(`/api/admin/registrations/${id}`, {
            method:  'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                phone:         document.getElementById('edit-phone').value,
                email:         document.getElementById('edit-email').value,
                address:       document.getElementById('edit-address').value,
                paymentStatus: document.getElementById('edit-status').value,
                adminNote:     document.getElementById('edit-admin-note').value.trim()
            })
        });
        setLoading(btn, false);
        closeModal('edit-modal');
        await Promise.all([loadRegistrations(), loadDashboardStats()]);
    } catch (err) {
        alert(`Update failed: ${err.message}`);
        setLoading(btn, false);
    }
});

async function deleteRegistration(id, btn) {
    if (!confirm('Delete this registration? This cannot be undone.')) return;
    try {
        btn.disabled = true;
        await apiFetchWithAuth(`/api/admin/registrations/${id}`, { method: 'DELETE' });
        await Promise.all([loadRegistrations(), loadDashboardStats()]);
    } catch (err) {
        alert(`Delete failed: ${err.message}`);
        btn.disabled = false;
    }
}

function exportCsv() {
    const token = localStorage.getItem('adminToken');
    const req   = new XMLHttpRequest();
    req.open('GET', '/api/admin/registrations/export' + adminYearQuery());
    req.setRequestHeader('Authorization', `Bearer ${token}`);
    req.responseType = 'blob';
    req.onload = () => {
        const a = document.createElement('a');
        a.href  = URL.createObjectURL(req.response);
        a.setAttribute('download', 'registrations.csv');
        a.click();
        URL.revokeObjectURL(a.href);
    };
    req.send();
}

// ── Performers ────────────────────────────────────────────────────────────────
let allPerformers = [];

async function loadPerformers() {
    showTableSkeleton('performers-tbody', 7, 5);
    try {
        const selected=adminSelectedEdition;
        const loaded=await apiFetchWithAuth('/api/admin/performers' + adminYearQuery()) || [];
        if(selected!==adminSelectedEdition)return;
        allPerformers=loaded;
        const tbody   = document.getElementById('performers-tbody');
        if (!tbody) return;
        if (!allPerformers.length) {
            tbody.innerHTML = `<tr><td colspan="7"
                style="text-align:center;padding:32px;color:var(--text-secondary)">
                No performer registrations.</td></tr>`;
            return;
        }
        tbody.innerHTML = allPerformers.map(p => `
            <tr>
                <td><strong>${escapeHtml(p.name)}</strong></td>
                <td style="font-size:13px">${escapeHtml(p.email)}</td>
                <td>${escapeHtml(p.performanceType || '-')}</td>
                <td style="text-align:center">${p.groupMemberCount || 1}</td>
                <td><span class="badge badge-${(p.approvalStatus || '').toLowerCase()}">
                    ${p.approvalStatus}</span></td>
                <td style="font-size:12px">${formatDateTime(p.registeredAt)}</td>
                <td>
                    <div class="actions">
                        <button class="btn btn-sm btn-outline" onclick="openPerformerEditModal('${p.id}')" title="Edit"><i class="fa-regular fa-pen-to-square"></i></button>
                        <button class="btn btn-sm btn-danger" onclick="deletePerformer('${p.id}')" title="Delete"><i class="fa-regular fa-trash-can"></i></button>
                    </div>
                </td>
            </tr>`).join('');
    } catch (e) { console.error('Performers load failed:', e); }
}

// ── Edit Performer Modal ───────────────────────────────────────────────────────
function openPerformerEditModal(id) {
    const p = allPerformers.find(x => String(x.id) === String(id));
    if (!p) return;
    document.getElementById('perf-edit-id').value              = p.id;
    document.getElementById('perf-edit-name').textContent      = p.name;
    document.getElementById('perf-edit-status').value          = p.approvalStatus || 'PENDING';
    document.getElementById('performer-modal').style.display   = 'flex';
}

document.getElementById('performer-edit-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn    = e.target.querySelector('[type="submit"]');
    setLoading(btn, true);
    const id     = document.getElementById('perf-edit-id').value;
    try {
        await apiFetchWithAuth(`/api/admin/performers/${id}/status`, {
            method:  'PUT',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({
                status:    document.getElementById('perf-edit-status').value,
                adminNote: document.getElementById('perf-admin-note').value.trim(), // ← ADDED
            })
        });
        setLoading(btn, false);
        closeModal('performer-modal');
        await loadPerformers();
    } catch (err) {
        alert(`Update failed: ${err.message}`);
        setLoading(btn, false);
    }
});


async function updatePerformerStatus(id, status) {
    try {
        await apiFetchWithAuth(`/api/admin/performers/${id}/status?status=${status}`,
            { method: 'PUT' });
        await loadPerformers();
    } catch (err) { alert(`Update failed: ${err.message}`); }
}

async function deletePerformer(id) {
    if (!confirm('Delete this performer registration?')) return;
    try {
        await apiFetchWithAuth(`/api/admin/performers/${id}`, { method: 'DELETE' });
        await loadPerformers();
    } catch (err) { alert(`Delete failed: ${err.message}`); }
}

// ── Modal / Utilities ─────────────────────────────────────────────────────────
function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
}

function logout() {
    localStorage.clear();
    window.location.href = 'admin.html';
}

// ── Change Password ──────────────────────────────────────────────────────────
document.getElementById('change-password-btn')?.addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('change-password-modal').style.display = 'flex';
    document.getElementById('cp-current').value  = '';
    document.getElementById('cp-new').value      = '';
    document.getElementById('cp-confirm').value  = '';
    document.getElementById('cp-error').style.display   = 'none';
    document.getElementById('cp-success').style.display = 'none';
});

function closeChangePasswordModal() {
    document.getElementById('change-password-modal').style.display = 'none';
}

function toggleCpVisibility(fieldId, eyeEl) {
    const input = document.getElementById(fieldId);
    input.type  = input.type === 'password' ? 'text' : 'password';
    eyeEl.style.opacity = input.type === 'text' ? '1' : '0.5';
}

async function submitChangePassword() {
    const currentPassword = document.getElementById('cp-current').value.trim();
    const newPassword     = document.getElementById('cp-new').value.trim();
    const confirmPassword = document.getElementById('cp-confirm').value.trim();
    const errorEl         = document.getElementById('cp-error');
    const successEl       = document.getElementById('cp-success');
    const btn             = document.getElementById('cp-submit-btn');

    errorEl.style.display   = 'none';
    successEl.style.display = 'none';

    if (!currentPassword || !newPassword || !confirmPassword) {
        errorEl.textContent   = 'All fields are required.';
        errorEl.style.display = 'block';
        return;
    }
    if (newPassword.length < 8) {
        errorEl.textContent   = 'New password must be at least 8 characters.';
        errorEl.style.display = 'block';
        return;
    }
    if (newPassword !== confirmPassword) {
        errorEl.textContent   = 'New passwords do not match.';
        errorEl.style.display = 'block';
        return;
    }

    setLoading(btn, true);
    try {
        await apiFetchWithAuth('/api/admin/auth/change-password', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ currentPassword, newPassword, confirmPassword })
        });
        successEl.textContent   = '✅ Password updated successfully!';
        successEl.style.display = 'block';
        setTimeout(closeChangePasswordModal, 1800);
    } catch (e) {
        errorEl.textContent   = e.message || 'Failed to change password.';
        errorEl.style.display = 'block';
    } finally {
        setLoading(btn, false);
    }
}

// ── Messages ─────────────────────────────────────────────────
let currentReplyId = null;

async function loadMessages() {
    const container = document.getElementById('messages-list');
    if (!container) return;
    container.innerHTML = '<div class="spinner" style="margin:0 auto"></div>';
    try {
        const messages = await apiFetchWithAuth('/api/contact/admin/messages');
        if (!messages.length) {
            container.innerHTML = '<p style="color:var(--text-secondary);padding:20px">No messages yet.</p>';
            return;
        }
        container.replaceChildren();
        for (const m of messages) {
            const item = contentNode('div', null, 'msg-item' + (m.read ? '' : ' msg-unread'));
            item.id = 'msg-' + m.id;
            const meta = contentNode('div', null, 'msg-meta');
            meta.append(contentNode('strong', m.name), contentNode('span', m.email, 'msg-email'), contentNode('span', new Date(m.submittedAt).toLocaleString(), 'msg-time'));
            if (m.answered) meta.append(contentNode('span', 'Replied', 'badge badge-confirmed'));
            else if (!m.read) meta.append(contentNode('span', 'New', 'badge badge-pending'));
            item.append(meta, contentNode('div', m.message, 'msg-body'));
            if (m.replyText) {
                const reply = contentNode('div', null, 'msg-reply-preview');
                reply.append(contentNode('strong', 'Your reply: '), document.createTextNode(m.replyText));
                item.append(reply);
            }
            const actions = contentNode('div'); actions.style.cssText = 'margin-top:10px;display:flex;gap:8px';
            const replyButton = contentNode('button', m.answered ? '✏️ Edit Reply' : '↩️ Reply', 'btn btn-sm btn-primary reply-btn');
            replyButton.type = 'button';
            replyButton.addEventListener('click', () => openReplyModal(m.id, m.name, m.email, m.message));
            actions.append(replyButton);
            if (!m.read) {
                const readButton = contentNode('button', 'Mark as Read', 'btn btn-sm btn-outline mark-read-btn');
                readButton.type = 'button'; readButton.addEventListener('click', () => markRead(m.id)); actions.append(readButton);
            }
            item.append(actions); container.append(item);
        }

    } catch (e) {
        container.innerHTML = '<p style="color:var(--error)">Failed to load messages.</p>';
    }
}


async function loadUnreadCount() {
    try {
        const data  = await apiFetchWithAuth('/api/contact/admin/messages/unread-count');
        const badge = document.getElementById('msg-unread-badge');
        if (!badge) return;
        if (data.count > 0) {
            badge.textContent    = data.count;
            badge.style.display  = 'inline-block';
        } else {
            badge.style.display  = 'none';
        }
    } catch (e) { /* silent */ }
}

async function markRead(id) {
    await apiFetchWithAuth(`/api/contact/admin/messages/${id}/read`, { method: 'PATCH' });
    loadMessages();
    loadUnreadCount();
}

function openReplyModal(id, name, email, message) {
    currentReplyId = id;
    const original = document.getElementById('reply-original');
    const messageText = contentNode('span', message); messageText.style.cssText = 'color:#666;font-size:13px';
    original.replaceChildren(contentNode('strong', name), document.createTextNode(' <' + email + '>'), document.createElement('br'), messageText);
    document.getElementById('reply-text').value = '';
    document.getElementById('reply-alert').innerHTML = '';
    document.getElementById('reply-modal').style.display = 'flex';
    // mark as read when opened
    apiFetchWithAuth(`/api/contact/admin/messages/${id}/read`, { method: 'PATCH' });
    loadUnreadCount();
}

async function submitReply() {
    const replyText = document.getElementById('reply-text').value.trim();
    const alertEl   = document.getElementById('reply-alert');
    const btn       = document.getElementById('reply-submit-btn');
    if (!replyText) {
        alertEl.innerHTML = '<p style="color:var(--error)">Please write a reply.</p>';
        return;
    }
    setLoading(btn, true);
    try {
        await apiFetchWithAuth(`/api/contact/admin/messages/${currentReplyId}/reply`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ replyText })
        });
        closeModal('reply-modal');
        loadMessages();
        loadUnreadCount();
    } catch (e) {
        alertEl.innerHTML = `<p style="color:var(--error)">${escapeHtml(e.message)}</p>`;
    } finally {
        setLoading(btn, false);
    }
}

// ── Events (What to Expect) ────────────────────────────────────────────────
async function loadEvents() {
    showTableSkeleton('events-tbody', 5, 4);
    try {
        const selected=adminSelectedEdition;
        const loaded=await apiFetchWithAuth('/api/admin/events' + adminYearQuery()) || [];
        if(selected!==adminSelectedEdition)return;
        allEvents=loaded;
        renderEventTable(allEvents);
    } catch (e) { console.error('Events load failed:', e); }
}

function renderEventTable(events) {
    const tbody = document.getElementById('events-tbody');
    if (!tbody) return;
    if (!events.length) {
        tbody.innerHTML = `<tr><td colspan="5"
            style="text-align:center;padding:32px;color:var(--text-secondary)">
            No events yet. Click "Create Event" to add one.</td></tr>`;
        return;
    }
    tbody.innerHTML = events.map(ev => `
        <tr>
            <td><span style="font-size:13px;text-transform:capitalize">${escapeHtml(ev.category || '')}</span></td>
            <td><strong>${escapeHtml(ev.title || '')}</strong></td>
            <td style="font-size:13px;color:var(--text-secondary);max-width:300px">
                ${escapeHtml(ev.description || '')}</td>
            <td style="text-align:center">
                ${ev.isHighlight ? '<span style="color:var(--warning);font-size:1.1rem">⭐</span>' : '—'}
            </td>
            <td>
                <div class="actions">
                    <button class="btn btn-sm btn-outline"
                            onclick="openEditEventModal(${ev.id})" title="Edit">
                        <i class="fa-regular fa-pen-to-square"></i>
                    </button>
                    <button class="btn btn-sm btn-danger"
                            onclick="deleteEvent(${ev.id}, this)" title="Delete">
                        <i class="fa-regular fa-trash-can"></i>
                    </button>
                </div>
            </td>
        </tr>`).join('');
}

function openEventModal() {
    document.getElementById('event-modal-title').textContent = '➕ Create Event';
    document.getElementById('event-id').value                = '';
    document.getElementById('event-category').value          = '';
    document.getElementById('event-title').value             = '';
    document.getElementById('event-description').value       = '';
    document.getElementById('event-highlight').checked       = false;
    document.getElementById('event-form-alert').innerHTML    = '';
    document.getElementById('event-modal').style.display     = 'flex';
}

function openEditEventModal(id) {
    const ev = allEvents.find(e => String(e.id) === String(id));
    if (!ev) return;
    document.getElementById('event-modal-title').textContent = '✏️ Edit Event';
    document.getElementById('event-id').value                = ev.id;
    document.getElementById('event-category').value          = ev.category    || '';
    document.getElementById('event-title').value             = ev.title       || '';
    document.getElementById('event-description').value       = ev.description || '';
    document.getElementById('event-highlight').checked       = ev.isHighlight || false;
    document.getElementById('event-form-alert').innerHTML    = '';
    document.getElementById('event-modal').style.display     = 'flex';
}

document.getElementById('event-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn  = document.getElementById('event-submit-btn');
    const id   = document.getElementById('event-id').value;
    const body = {
        category:    document.getElementById('event-category').value,
        title:       document.getElementById('event-title').value.trim(),
        description: document.getElementById('event-description').value.trim(),
        isHighlight: document.getElementById('event-highlight').checked,
        eventYear:   adminSelectedYear,
        eventEditionId: adminSelectedEdition
    };
    setLoading(btn, true);
    try {
        const url    = id ? `/api/admin/events/${id}` : '/api/admin/events';
        const method = id ? 'PUT' : 'POST';
        await apiFetchWithAuth(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(body)
        });
        setLoading(btn, false);
        closeModal('event-modal');
        await loadEvents();
    } catch (err) {
        document.getElementById('event-form-alert').innerHTML =
            `<p style="color:var(--error);margin-top:8px">${err.message}</p>`;
        setLoading(btn, false);
    }
});

async function deleteEvent(id, btn) {
    if (!confirm('Delete this event? It will be removed from the home page.')) return;
    try {
        btn.disabled = true;
        await apiFetchWithAuth(`/api/admin/events/${id}`, { method: 'DELETE' });
        await loadEvents();
    } catch (err) {
        alert(`Delete failed: ${err.message}`);
        btn.disabled = false;
    }
}


// Sidebar toggle for mobile
document.getElementById('sidebar-toggle')?.addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('open');
    document.getElementById('sidebar-overlay').classList.toggle('active');
});

document.getElementById('sidebar-overlay')?.addEventListener('click', () => {
    document.getElementById('sidebar').classList.remove('open');
    document.getElementById('sidebar-overlay').classList.remove('active');
});