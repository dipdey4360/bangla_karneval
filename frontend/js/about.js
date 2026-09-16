document.addEventListener('DOMContentLoaded', async () => {
    await loadAboutConfig();
});

async function loadAboutConfig() {
    try {
        const config = await apiFetch('/api/config/current');
        const aboutText = document.getElementById('about-text');
        const eventDate = document.getElementById('about-event-date');
        const eventLoc = document.getElementById('about-event-location');
        if (aboutText) aboutText.textContent = config.aboutText || '';
        if (eventDate) eventDate.textContent = formatDate(config.eventDate);
        if (eventLoc) eventLoc.textContent = config.eventLocation || '';
    } catch (e) {
        console.error('Failed to load about config:', e);
    }
}
