document.addEventListener('DOMContentLoaded', loadAboutConfig);

async function loadAboutConfig() {
    try {
        const organisation = await apiFetch('/api/organisation');
        const container = document.getElementById('about-text');
        if (!container || !organisation?.story?.trim()) return;
        // Plain text from the admin editor becomes separate, safe paragraphs.
        const paragraphs = organisation.story.trim().split(/\n\s*\n/).map(text => {
            const paragraph = document.createElement('p');
            const emphasis = 'Bangla Karneval e.V.—with you, and for you.';
            const index = text.indexOf(emphasis);
            if (index < 0) paragraph.textContent = text;
            else {
                const strong = document.createElement('strong');
                strong.textContent = emphasis;
                paragraph.append(document.createTextNode(text.slice(0, index)), strong,
                    document.createTextNode(text.slice(index + emphasis.length)));
            }
            return paragraph;
        });
        container.replaceChildren(...paragraphs);
    } catch (error) {
        // Keep the approved story visible if organisation settings cannot be loaded.
        console.warn('Organisation story could not be loaded', error);
    }
}
