let performerEventYear = null;
document.addEventListener('DOMContentLoaded', () => {
    getActiveEventConfig().then(config => { performerEventYear = config.eventYear; })
        .catch(() => showAlert('performer-alert', 'Event details could not be loaded. Refresh before applying.', 'error'));
    document.getElementById('performer-form')
        ?.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (performerEventYear === null) { showAlert('performer-alert', 'Refresh the page to load the event year before applying.', 'error'); return; }
            if (!e.target.reportValidity()) return;
            const btn = e.target.querySelector('[type="submit"]');
            setLoading(btn, true);

            const performanceType = document.querySelector(
                'input[name="performanceType"]:checked'
            )?.value;
            if (!performanceType) {
                showAlert('performer-alert', 'Please select a performance type.', 'error');
                setLoading(btn, false);
                return;
            }

            // Collect group members
            const groupMembers = [];
            let valid = true;
            document.querySelectorAll('.group-member-card').forEach((card, i) => {
                const name   = card.querySelector('.member-name')?.value?.trim();
                const dob    = card.querySelector('.member-dob')?.value;
                const gender = card.querySelector('.member-gender')?.value;
                if (!name) {
                    showAlert('performer-alert',
                        `Please enter the name for group member ${i + 1}.`, 'error');
                    valid = false;
                    return;
                }
                groupMembers.push({ name, dateOfBirth: dob || null, gender: gender || null });
            });

            if (!valid) { setLoading(btn, false); return; }

            const payload = {
                eventYear: performerEventYear,
                name:                   document.getElementById('p-name').value.trim(),
                email:                  document.getElementById('p-email').value.trim(),
                phone:                  document.getElementById('p-phone').value.trim(),
                dateOfBirth:            document.getElementById('p-dob').value || null,
                address:                document.getElementById('p-address').value.trim(),
                performanceType,
                performanceDescription: document.getElementById('p-description').value.trim(),
                groupMemberCount:       parseInt(document.getElementById('p-group-count').value) || 1,
                groupMembers
            };

            try {
                await apiFetch('/api/register/performer', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                document.getElementById('performer-form-card').style.display = 'none';
                document.getElementById('performer-success').style.display   = 'block';
            } catch (err) {
                showAlert('performer-alert', `Submission failed: ${err.message}`, 'error');
                setLoading(btn, false);
            }
        });
});

// ── Dynamically render group member cards ─────────────────────────────────────
function updateGroupMembers() {
    const count     = parseInt(document.getElementById('p-group-count').value) || 1;
    const section   = document.getElementById('group-members-section');
    const container = document.getElementById('group-members-container');

    // Hide section if only 1 performer
    if (count <= 1) {
        section.style.display = 'none';
        container.innerHTML   = '';
        return;
    }

    section.style.display = 'block';

    // Build cards for members 2..N (member 1 is the main registrant)
    const yesterday = new Date(); yesterday.setDate(yesterday.getDate() - 1);
    const today = `${yesterday.getFullYear()}-${String(yesterday.getMonth()+1).padStart(2,'0')}-${String(yesterday.getDate()).padStart(2,'0')}`;
    let html    = '';
    for (let i = 2; i <= count; i++) {
        html += `
        <div class="group-member-card">
            <div class="group-member-header">
                <span class="group-member-number">👤 Performer ${i}</span>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label">Full Name *</label>
                    <input type="text" class="form-input member-name"
                           placeholder="Full name" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Gender</label>
                    <select class="form-select member-gender">
                        <option value="">— Select —</option>
                        <option value="MALE">Male</option>
                        <option value="FEMALE">Female</option>
                        <option value="OTHER">Other</option>
                        <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                    </select>
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label">
                        Date of Birth
                        <span class="member-age-display"
                              style="font-weight:400;color:var(--text-secondary);font-size:13px"></span>
                    </label>
                    <input type="date" class="form-input member-dob"
                           max="${today}" onchange="showMemberAge(this)">
                </div>
                <div class="form-group">
                    <!-- spacer -->
                </div>
            </div>
        </div>`;
    }
    container.innerHTML = html;
}

function showMemberAge(inputEl) {
    const dob     = inputEl.value;
    const display = inputEl.closest('.form-group').querySelector('.member-age-display');
    if (!display) return;
    if (!dob) { display.textContent = ''; return; }
    const age = calculateAgeFromDob(dob);
    display.textContent = `— ${age} years old`;
}

// ── Age calculator (shared with registration.js via common.js scope) ──────────
function calculateAgeFromDob(dobStr) {
    if (!dobStr) return null;
    const today = new Date();
    const dob   = new Date(dobStr);
    let age     = today.getFullYear() - dob.getFullYear();
    const m     = today.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) age--;
    return age;
}
