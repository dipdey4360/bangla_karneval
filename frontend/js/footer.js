document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('footer-container');
    if (!container) return;

    fetch('/components/footer.html')
        .then(res => res.text())
        .then(html => {
            container.innerHTML = html;
            loadFooterSponsors();
        })
        .catch(err => console.error('Footer load error:', err));
});

async function loadFooterSponsors() {
    const grid = document.getElementById('footer-sponsor-cards');
    if (!grid) return;

    try {
        const sponsors = await apiFetch('/api/sponsors');

        if (!sponsors.length) {
            grid.closest('.footer-sponsors').style.display = 'none';
            return;
        }

        grid.innerHTML = sponsors.map(s => `
            <div class="sponsor-card">
                <div class="sponsor-logo-box">
                    ${s.logoPath
            ? `<img src="${s.logoPath}" alt="${s.name}">`
            : `<i class="fa-solid fa-building" style="font-size:1.8rem;color:#ccc"></i>`}
                </div>
                <div class="sponsor-name">
                    ${s.websiteUrl
            ? `<a href="${s.websiteUrl}" target="_blank"
                              style="color:inherit;text-decoration:none">${s.name}</a>`
            : s.name}
                </div>
                ${s.address     ? `<div class="sponsor-address"><i class="fa-solid fa-location-dot"></i> ${s.address}</div>` : ''}
                ${s.description ? `<div class="sponsor-address"><i class="fa-solid fa-circle-info"></i> ${s.description}</div>` : ''}
            </div>
        `).join('');

    } catch (e) {
        grid.closest('.footer-sponsors').style.display = 'none';
    }
}
