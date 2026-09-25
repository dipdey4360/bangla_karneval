let participantCount = 0;
let pricePerPerson   = 10;
let registrationEventYear = null;
let registrationEditionId = null, registrationEventVersion = null;

document.addEventListener('DOMContentLoaded', async () => {
    mountMembershipCheck(document.getElementById('primary-membership'), document.getElementById('primary-name'));
    setupForm();
    document.getElementById('add-participant-btn')
        ?.addEventListener('click', addParticipant);
    await loadPrice();
});

async function loadPrice() {
    try {
        const config   = await getActiveEventConfig();
        const requestedEdition = new URLSearchParams(location.search).get('edition');
        if (requestedEdition && requestedEdition !== String(config.eventEditionId)) {
            document.getElementById('registration-form').hidden = true;
            showAlert('form-alert', 'This event is no longer active. Please return to Home and select the current event.', 'info');
            return;
        }
        document.querySelectorAll('[data-event-payment]').forEach(el=>el.textContent=config.paymentInstructions || 'Contact the organisers for donation details. Include your registration reference with your donation.');
        if(config.registrationEnabled===false){document.getElementById('registration-form').hidden=true;showAlert('form-alert','Registration is currently closed for this event.','info');}
        registrationEventYear = config.eventYear;
        registrationEditionId=config.eventEditionId; registrationEventVersion=config.version;
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

// ── Donation method expand/collapse ────────────────────────────────────────────
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
                <label class="form-label">Relationship</label>
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
    const memberHost = document.createElement('div');
    card.appendChild(memberHost);
    mountMembershipCheck(memberHost, card.querySelector('.participant-name'));
    card.membershipHost = memberHost;
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

    const primaryAmount = memberAmount(document.getElementById('primary-membership'), pricePerPerson);
    let total = primaryAmount;
    if (primDisplay) primDisplay.textContent = formatCurrency(primaryAmount);

    let rows = `<div class="price-row">
        <span>Primary registrant</span>
        <span>${formatCurrency(primaryAmount)}</span>
    </div>`;

    document.querySelectorAll('.additional-participant-card').forEach((card, i) => {
        const dobEl  = card.querySelector('.participant-dob');
        const dob    = dobEl?.value || '';
        const age    = dob ? calculateAgeFromDob(dob) : null;
        const amount = memberAmount(card.membershipHost, (age !== null && age < 18) ? 0 : pricePerPerson);
        if (dob) total += amount;
        rows += `<div class="price-row">
            <span>Participant ${i + 1}
                ${age !== null ? `(${age} yrs)` : ''}
                ${age !== null && age < 18 ? '🆓' : ''}
            </span>
            <span>${dob ? formatCurrency(amount) : '—'}</span>
        </div>`;
    });

    const saving = [memberSaving(document.getElementById('primary-membership'), pricePerPerson),
        ...Array.from(document.querySelectorAll('.additional-participant-card'), card => {
            const dob = card.querySelector('.participant-dob').value;
            return dob && calculateAgeFromDob(dob) >= 18 ? memberSaving(card.membershipHost, pricePerPerson) : 0;
        })].reduce((a,b) => a+b, 0);
    if (saving > 0) rows += `<div class="price-row"><span>Member savings</span><span>−${formatCurrency(saving)}</span></div>`;
    displayedTotal = Math.round(total * 100) / 100;
    summaryBody.innerHTML = rows;
    totalEl.textContent   = formatCurrency(total);
}

// ── Form submit ───────────────────────────────────────────────────────────────
function setupForm() {
    document.getElementById('registration-form')
        ?.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!e.target.reportValidity()) return;
            if (registrationEventYear === null) { showAlert('form-alert', 'Refresh the page to load the event year before registering.', 'error'); return; }
            if (!membershipsReady()) {
                showAlert('form-alert', 'Verify membership for each participant who selected Yes, or select No to continue without a member discount.', 'error');
                return;
            }
            updatePriceSummary();
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
                    membershipId: selectedMembershipId(card.membershipHost),
                    dateOfBirth: dob,
                    gender:      gender   || null,
                    relation:    relation || null
                });
            });

            if (!valid) {
                showAlert('form-alert', 'Please enter a name and date of birth for each participant.', 'error');
                setLoading(btn, false);
                return;
            }

            const paymentMethod = document.querySelector(
                'input[name="paymentMethod"]:checked'
            )?.value;
            if (!paymentMethod) {
                showAlert('form-alert', 'Please select a donation method.', 'error');
                setLoading(btn, false);
                return;
            }

            const payload = {
                membershipId: selectedMembershipId(document.getElementById('primary-membership')),
                expectedTotal: displayedTotal,
                eventYear: registrationEventYear,
                eventEditionId: registrationEditionId, eventVersion: registrationEventVersion,
                primaryName:            document.getElementById('primary-name').value.trim(),
                email:                  document.getElementById('primary-email').value.trim(),
                primaryDateOfBirth:     document.getElementById('primary-dob').value,
                gender:                 document.getElementById('primary-gender').value || null,
                phone:                  document.getElementById('primary-phone').value.trim(),
                address:                document.getElementById('primary-address').value.trim(),
                paymentMethod,
                consent: document.getElementById('registration-consent').checked,
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
                document.getElementById('success-payment').textContent   = paymentMethod === 'PAYPAL' ? 'PayPal' : 'Bank transfer';
                success.scrollIntoView({ behavior: 'smooth' });

            } catch (err) {
                showAlert('form-alert', `Registration failed: ${err.message}`, 'error');
                setLoading(btn, false);
            }
        });
}

// Each participant verifies independently, including either partner of a couple membership.
let displayedTotal = 0;
let membershipControlCounter = 0;
function mountMembershipCheck(host, nameInput) {
    const key = `membership-${++membershipControlCounter}`;
    host.classList.add('membership-check');
    host.innerHTML = `<div class="form-group"><label class="form-label" for="${key}-choice">Are you already a member?</label>
        <select id="${key}-choice" class="form-select member-choice"><option value="no">No</option><option value="yes">Yes</option></select></div>
        <div class="member-details" hidden><label class="form-label" for="${key}-id">Membership ID</label>
        <input id="${key}-id" class="form-input member-id" maxlength="40" placeholder="BKM-00001" autocomplete="off" spellcheck="false" disabled>
        <button type="button" class="btn btn-outline member-verify" style="margin-top:8px">Verify membership</button></div>
        <p class="member-status" role="status" aria-live="polite"></p>`;
    const choice = host.querySelector('.member-choice'), input = host.querySelector('.member-id');
    const details = host.querySelector('.member-details'), button = host.querySelector('.member-verify');
    const status = host.querySelector('.member-status');
    const state = {choice, input, nameInput, verifiedKey:null, percent:0, sequence:0};
    host.membershipState = state;
    const reset = () => {
        state.sequence++; state.verifiedKey = null; state.percent = 0;
        button.disabled = false;
        status.textContent = choice.value === 'yes' ? 'Enter your full name and membership ID, then select Verify membership.' : '';
        updatePriceSummary();
    };
    choice.addEventListener('change', () => {
        details.hidden = choice.value !== 'yes'; input.disabled = details.hidden; input.required = !details.hidden;
        reset();
    });
    input.addEventListener('input', reset); nameInput.addEventListener('input', reset);
    input.addEventListener('keydown', event => {
        if (event.key === 'Enter') { event.preventDefault(); if (!button.disabled) button.click(); }
    });
    button.addEventListener('click', async () => {
        reset();
        if (!nameInput.value.trim() || !input.value.trim()) { status.textContent = 'Enter your full name and membership ID first.'; return; }
        const key = membershipKey(state), sequence = state.sequence;
        button.disabled = true; status.textContent = 'Verifying membership…';
        try {
            const result = await apiFetch('/api/register/verify-membership', {
                method:'POST', headers:{'Content-Type':'application/json'}, cache:'no-store',
                signal: typeof AbortSignal !== 'undefined' && AbortSignal.timeout ? AbortSignal.timeout(15000) : undefined,
                body:JSON.stringify({name:nameInput.value.trim(), membershipId:input.value.trim()})
            });
            if (sequence !== state.sequence || key !== membershipKey(state) || choice.value !== 'yes') return;
            const percent = Number(result.discountPercent);
            if (!result.verified || !Number.isFinite(percent) || percent < 0 || percent > 100) throw new Error('Membership could not be verified. Please try again.');
            state.verifiedKey = key; state.percent = percent;
            status.textContent = percent > 0 ? `Membership verified. A ${percent}% discount applies to this participant’s donation.` : 'Membership verified. No member discount is currently offered.';
            updatePriceSummary();
        } catch (error) {
            if (sequence === state.sequence) status.textContent = error.name === 'TimeoutError'
                ? 'Verification timed out. Please check your connection and try again.'
                : error.message || 'Verification failed. Please try again.';
        } finally { if (sequence === state.sequence) button.disabled = false; }
    });
}
function membershipKey(state) { return JSON.stringify([state.nameInput.value.trim(), state.input.value.trim().toUpperCase()]); }
function selectedMembershipId(host) {
    const state = host?.membershipState;
    return state?.choice.value === 'yes' && state.verifiedKey === membershipKey(state) ? state.input.value.trim().toUpperCase() : null;
}
function memberSaving(host, base) {
    return selectedMembershipId(host) ? Math.round((base * host.membershipState.percent / 100 + Number.EPSILON) * 100) / 100 : 0;
}
function memberAmount(host, base) { return base - memberSaving(host, base); }
function membershipsReady() {
    return Array.from(document.querySelectorAll('.membership-check')).every(host => host.membershipState.choice.value !== 'yes' || selectedMembershipId(host));
}
