document.addEventListener('DOMContentLoaded', async () => {
    await loadContactDetails();
    document.getElementById('contact-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = e.target.querySelector('[type="submit"]');
        setLoading(btn, true);

        const payload = {
            name:    document.getElementById('c-name').value.trim(),
            email:   document.getElementById('c-email').value.trim().toLowerCase(),  // ← trim + lowercase
            message: document.getElementById('c-message').value.trim()
        };

        // ── Frontend validation before sending ────────────────────
        if (!payload.name || !payload.email || !payload.message) {
            showAlert('contact-alert', 'Please complete all required fields.', 'error');
            setLoading(btn, false);
            return;
        }
        if (payload.message.length < 10) {
            showAlert('contact-alert', 'Please enter a message of at least 10 characters.', 'error');
            setLoading(btn, false);
            return;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
            showAlert('contact-alert', 'Please enter a valid email address.', 'error');
            setLoading(btn, false);
            return;
        }

        try {
            await apiFetch('/api/contact', {
                method:  'POST',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify(payload)
            });
            showAlert('contact-alert', 'Your message has been sent. We will respond soon.', 'success');
            e.target.reset();
            setLoading(btn, false);
        } catch (err) {
            showAlert('contact-alert', `Failed to send: ${err.message}`, 'error');
            setLoading(btn, false);
        }
    });
});

async function loadContactDetails() {
    try {
        const config = await apiFetch('/api/organisation');
        const event = await getActiveEventConfig();
        const set = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val || ''; };
        set('contact-phone',    config.contactPhone);
        set('contact-email',    config.contactEmail);
        set('contact-location', event.eventLocation);

        const phoneLink = document.getElementById('contact-phone-link');
        const emailLink = document.getElementById('contact-email-link');
        if (phoneLink) phoneLink.href = `tel:${config.contactPhone}`;
        if (emailLink) emailLink.href = `mailto:${config.contactEmail}`;

        const fbLink = document.getElementById('contact-facebook');
        const igLink = document.getElementById('contact-instagram');
        if (fbLink && config.contactFacebook)  fbLink.href = safeWebUrl(config.contactFacebook) || '#';
        if (igLink && config.contactInstagram) igLink.href = safeWebUrl(config.contactInstagram) || '#';

    } catch (e) { console.error('Failed to load contact details:', e); }
}
