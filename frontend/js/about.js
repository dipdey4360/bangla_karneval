document.addEventListener('DOMContentLoaded', async () => {
    await loadAboutConfig();
});

async function loadAboutConfig() {
    try {
        const [config, organisation] = await Promise.all([getActiveEventConfig(), apiFetch('/api/organisation')]);
        const aboutText = document.getElementById('about-text');
        const eventDate = document.getElementById('about-event-date');
        const eventLoc = document.getElementById('about-event-location');
        if (aboutText) aboutText.textContent = organisation.story || 'Bangla Karneval e.V. brings our community together through culture and shared celebrations.';
        if (eventDate) eventDate.textContent = formatDate(config.eventDate);
        if (eventLoc) eventLoc.textContent = config.eventLocation || '';
    } catch (e) {
        console.error('Failed to load about config:', e);
    }
}
