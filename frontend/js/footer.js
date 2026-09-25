document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('footer-container');
    if (!container) return;

    fetch('/components/footer.html', {cache:'no-store'})
        .then(res => res.text())
        .then(html => {
            container.innerHTML = html;
            updateEventYearLabels();
            loadFooterSponsors();
            loadClubSocialLinks();
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

        grid.replaceChildren();
        for (const s of sponsors) {
            const card = contentNode('div', null, 'sponsor-card');
            const logo = contentNode('div', null, 'sponsor-logo-box');
            const imageUrl = safeWebUrl(s.logoPath);
            if (imageUrl) { const image = contentNode('img'); image.src=imageUrl; image.alt=s.name || ''; logo.append(image); }
            else { const icon=contentNode('i',null,'fa-solid fa-building'); icon.style.cssText='font-size:1.8rem;color:#ccc'; logo.append(icon); }
            const name = contentNode('div', null, 'sponsor-name');
            const website = safeWebUrl(s.websiteUrl);
            if (website) { const link = contentNode('a',s.name); link.href=website; link.target='_blank'; link.rel='noopener noreferrer'; link.style.cssText='color:inherit;text-decoration:none'; name.append(link); }
            else name.textContent=s.name || '';
            card.append(logo,name);
            for (const [value,icon] of [[s.address,'fa-location-dot'],[s.description,'fa-circle-info']]) {
                if (value) { const detail=contentNode('div',null,'sponsor-address'); detail.append(contentNode('i',null,'fa-solid '+icon),document.createTextNode(' '+value)); card.append(detail); }
            }
            grid.append(card);
        }

    } catch (e) {
        grid.closest('.footer-sponsors').style.display = 'none';
    }
}

async function loadClubSocialLinks() {
    const year=document.getElementById('club-copyright-year');if(year)year.textContent=new Date().getFullYear();
    try {
        const organisation=await apiFetch('/api/organisation');
        for(const [id,key] of [['club-facebook','contactFacebook'],['club-instagram','contactInstagram']]) {
            const link=document.getElementById(id),url=safeWebUrl(organisation?.[key]);
            if(link && url){link.href=url;link.hidden=false;}
        }
    } catch(error){console.warn('Social links could not be loaded',error);}
}
