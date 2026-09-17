let participantCount = 0;
let pricePerPerson   = 10;
let registrationEventYear = null;

document.addEventListener('DOMContentLoaded', async () => {
    await loadPrice();
    setupForm();
    document.getElementById('add-participant-btn')
        ?.addEventListener('click', addParticipant);
});

async function loadPrice() {
    try {
        const config   = await getActiveEventConfig();
        registrationEventYear = config.eventYear;
        pricePerPerson = Number(config.pricePerPerson ?? 10);
        document.getElementById('price-label').textContent = formatCurrency(pricePerPerson);
    } catch (e) { showAlert('form-alert', 'Event details could not be loaded. Refresh before registering.', 'error'); }
    updatePriceSummary();
}

// ── Age helper ────────────────────────────────────────────────────────────────
function calculateAgeFromDob(dobStr) {
    if (!dobStr) return null;
    const today = new Date();
    const dob   = new Date(dobStr);
    let age     = today.getFullYear() - dob.getFullYear();
    const m     = today.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) age--;
    return age;
}

function onPrimaryDobChange() {
    const dob     = document.getElementById('primary-dob').value;
    const display = document.getElementById('primary-age-display');
    if (!dob) { display.textContent = ''; updatePriceSummary(); return; }
    const age = calculateAgeFromDob(dob);
    display.textContent = `— ${age} years old`;
    updatePriceSummary();
}

// ── Payment method expand/collapse ────────────────────────────────────────────
function onPaymentMethodChange() {
    document.getElementById('paypal-details').style.display = 'none';
    document.getElementById('bank-details').style.display   = 'none';

    const selected = document.querySelector('input[name="paymentMethod"]:checked')?.value;
    if (selected === 'PAYPAL')         document.getElementById('paypal-details').style.display = 'block';
    if (selected === 'BANK_TRANSFER')  document.getElementById('bank-details').style.display   = 'block';
}

// ── Copy to clipboard ─────────────────────────────────────────────────────────
function copyText(text) {
    navigator.clipboard.writeText(text).then(() => {
        showAlert('form-alert', 'Copied to clipboard!', 'success');
        setTimeout(() => {
            const el = document.getElementById('form-alert');
            if (el) el.innerHTML = '';
        }, 2000);
    });
}

// ── Additional participants ───────────────────────────────────────────────────
function addParticipant() {
    participantCount++;
    const idx       = participantCount;
    const container = document.getElementById('additional-participants-container');
    const today     = new Date().toISOString().split('T')[0];
    const card      = document.createElement('div');
    card.className  = 'additional-participant-card';
    card.id         = `participant-${idx}`;
    card.innerHTML  = `
        <button type="button" class="remove-participant"
                onclick="removeParticipant(${idx})" title="Remove">×</button>
        <h5 style="margin-bottom:12px;color:var(--primary-red)">Participant ${idx}</h5>
        <div class="form-row">
            <div class="form-group">
                <label class="form-label">Full Name *</label>
                <input type="text" class="form-input participant-name"
                       placeholder="Full name" required>
            </div>
            <div class="form-group">
                <label class="form-label">
                    Date of Birth *
                    <span class="participant-age-display"
                          style="font-weight:400;color:var(--text-secondary);font-size:13px"></span>
                </label>
                <input type="date" class="form-input participant-dob"
                       max="${today}" required
                       onchange="onParticipantDobChange(this)">
            </div>
        </div>
        <div class="form-row">
            <div class="form-group">
                <label class="form-label">Gender</label>
                <select class="form-select participant-gender">
                    <option value="">— Select —</option>
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                    <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Relation</label>
                <select class="form-select participant-relation">
                    <option value="">— Select —</option>
                    <option value="FAMILY">Family Member</option>
                    <option value="SPOUSE">Spouse / Partner</option>
                    <option value="CHILD">Child</option>
                    <option value="PARENT">Parent</option>
                    <option value="SIBLING">Sibling</option>
                    <option value="FRIEND">Friend</option>
                    <option value="COLLEAGUE">Colleague</option>
                    <option value="OTHER">Other</option>
                </select>
            </div>
        </div>`;
    container?.appendChild(card);
    updatePriceSummary();
}

function onParticipantDobChange(inputEl) {
    const dob     = inputEl.value;
    const display = inputEl.closest('.form-group').querySelector('.participant-age-display');
    if (!dob) { if (display) display.textContent = ''; updatePriceSummary(); return; }
    const age = calculateAgeFromDob(dob);
    if (display) {
        display.textContent = age < 18 ? `— ${age} yrs (🆓 Free)` : `— ${age} yrs`;
    }
    updatePriceSummary();
}

function removeParticipant(idx) {
    document.getElementById(`participant-${idx}`)?.remove();
    updatePriceSummary();
}

// ── Live price summary ────────────────────────────────────────────────────────
function updatePriceSummary() {
    const summaryBody = document.getElementById('price-summary-body');
    const totalEl     = document.getElementById('total-amount');
    const primDisplay = document.getElementById('primary-price-display');
    if (!summaryBody || !totalEl) return;

    let total = pricePerPerson;
    if (primDisplay) primDisplay.textContent = formatCurrency(pricePerPerson);

    let rows = `<div class="price-row">
        <span>Primary registrant</span>
        <span>${formatCurrency(pricePerPerson)}</span>
    </div>`;

    document.querySelectorAll('.additional-participant-card').forEach((card, i) => {
        const dobEl  = card.querySelector('.participant-dob');
        const dob    = dobEl?.value || '';
        const age    = dob ? calculateAgeFromDob(dob) : null;
        const amount = (age !== null && age < 18) ? 0 : pricePerPerson;
        if (dob) total += amount;
        rows += `<div class="price-row">
            <span>Participant ${i + 1}
                ${age !== null ? `(${age} yrs)` : ''}
                ${age !== null && age < 18 ? '🆓' : ''}
            </span>
            <span>${dob ? formatCurrency(amount) : '—'}</span>
        </div>`;
    });

    summaryBody.innerHTML = rows;
    totalEl.textContent   = formatCurrency(total);
}

// ── Form submit ───────────────────────────────────────────────────────────────
function setupForm() {
    document.getElementById('registration-form')
        ?.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (registrationEventYear === null) { showAlert('form-alert', 'Refresh the page to load the event year before registering.', 'error'); return; }
            const btn = e.target.querySelector('[type="submit"]');
            setLoading(btn, true);

            const additionalParticipants = [];
            let valid = true;
            document.querySelectorAll('.additional-participant-card').forEach(card => {
                const name     = card.querySelector('.participant-name')?.value?.trim();
                const dob      = card.querySelector('.participant-dob')?.value;
                const gender   = card.querySelector('.participant-gender')?.value;
                const relation = card.querySelector('.participant-relation')?.value;
                if (!name || !dob) { valid = false; return; }
                additionalParticipants.push({
                    name,
                    dateOfBirth: dob,
                    gender:      gender   || null,
                    relation:    relation || null
                });
            });

            if (!valid) {
                showAlert('form-alert', 'Please fill in all participant details.', 'error');
                setLoading(btn, false);
                return;
            }

            const paymentMethod = document.querySelector(
                'input[name="paymentMethod"]:checked'
            )?.value;
            if (!paymentMethod) {
                showAlert('form-alert', 'Please select a payment method.', 'error');
                setLoading(btn, false);
                return;
            }

            const payload = {
                eventYear: registrationEventYear,
                primaryName:            document.getElementById('primary-name').value.trim(),
                email:                  document.getElementById('primary-email').value.trim(),
                primaryDateOfBirth:     document.getElementById('primary-dob').value,
                gender:                 document.getElementById('primary-gender').value || null,
                phone:                  document.getElementById('primary-phone').value.trim(),
                address:                document.getElementById('primary-address').value.trim(),
                paymentMethod,
                additionalParticipants
            };

            try {
                const data = await apiFetch('/api/register/general', {
                    method:  'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body:    JSON.stringify(payload)
                });

                document.getElementById('registration-form-card').style.display = 'none';
                document.getElementById('price-summary-card').style.display     = 'none';
                const success = document.getElementById('success-overlay');
                success.classList.add('show');
                document.getElementById('success-ref-code').textContent  = data.referenceCode;
                document.getElementById('success-amount').textContent    = formatCurrency(data.totalAmount);
                document.getElementById('success-payment').textContent   = paymentMethod.replace('_', ' ');
                success.scrollIntoView({ behavior: 'smooth' });

            } catch (err) {
                showAlert('form-alert', `Registration failed: ${err.message}`, 'error');
                setLoading(btn, false);
            }
        });
}
