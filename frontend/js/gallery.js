/* ══════════════════════════════════════════════════════════════
   GALLERY — gallery.js
   ══════════════════════════════════════════════════════════════ */

let currentEventYear=null, galleryLoadId=0;
document.addEventListener('DOMContentLoaded',async()=>{await initGallery();setupLightbox();});
async function initGallery(){
 try {
 const c=await getActiveEventConfig(); currentEventYear=c.eventYear;
 document.getElementById('highlights-title').textContent=c.title+' Highlights';
 const items=await apiFetch('/api/editions/'+c.eventEditionId+'/gallery?highlight=true');
 renderGallery(items,'highlights-grid','No highlights added for this event yet.');
 const editions=await apiFetch('/api/editions'), tabs=document.getElementById('year-tabs'); tabs.replaceChildren();
 for(const e of editions){const tab=contentNode('button',e.title,'year-tab');tab.type='button';tab.dataset.edition=e.eventEditionId;tab.addEventListener('click',()=>loadEditionGallery(e.eventEditionId));tabs.append(tab);}
 await loadEditionGallery(c.eventEditionId);
 }catch(e){document.getElementById('highlights-grid').textContent='Unable to load gallery. Please refresh.';}
}
async function loadEditionGallery(id){
 const request=++galleryLoadId;
 document.querySelectorAll('.year-tab').forEach(t=>t.classList.toggle('active',Number(t.dataset.edition)===Number(id)));
 try{const items=await apiFetch('/api/editions/'+id+'/gallery'); if(request===galleryLoadId)renderGallery(items,'year-gallery-grid','No photos or videos uploaded for this event yet.');}
 catch(e){if(request===galleryLoadId)document.getElementById('year-gallery-grid').textContent='Unable to load this event gallery.';}
}

function renderGallery(items, containerId, emptyMessage = 'No images yet.') {
    const container = document.getElementById(containerId);
    if (!container) return;
    if (!items || items.length === 0) {
        container.innerHTML = `<p class="gallery-empty">${emptyMessage}</p>`;
        return;
    }
    container.replaceChildren();
    items.forEach(item => {
        const card = contentNode('button', null, 'gallery-item'); card.type = 'button';
        const video = item.mediaType === 'VIDEO';
        card.setAttribute('aria-label', (video ? 'Play video: ' : 'View image: ') + (item.caption || 'Gallery item'));
        const media = contentNode(video ? 'video' : 'img');
        media.src = safeWebUrl(item.url);
        if (video) { media.muted = true; media.preload = 'metadata'; media.playsInline = true; }
        else { media.alt = item.caption || ''; media.loading = 'lazy'; }
        const overlay = contentNode('div', null, 'gallery-item-overlay');
        overlay.append(contentNode('span', (video ? '▶ ' : '') + (item.caption || ''), 'gallery-item-caption'));
        card.append(media, overlay); card.addEventListener('click', () => openLightbox(item));
        container.append(card);
    });
}

/* ── Lightbox ───────────────────────────────────────────────── */
function setupLightbox() {
    const lb = document.getElementById('lightbox');
    if (!lb) return;
    lb.addEventListener('click', (e) => { if (e.target === lb) closeLightbox(); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeLightbox(); });
}

function openLightbox(item) {
    const lb  = document.getElementById('lightbox');
    const img = document.getElementById('lightbox-image');
    const cap = document.getElementById('lightbox-caption');
    if (!lb || !img) return;
    const video = document.getElementById('lightbox-video');
    video.pause(); video.removeAttribute('src'); video.load();
    const isVideo = item.mediaType === 'VIDEO';
    img.hidden = isVideo; video.hidden = !isVideo;
    img.removeAttribute('src');
    if (isVideo) video.src = safeWebUrl(item.url);
    else { img.src = safeWebUrl(item.url); img.alt = item.caption || 'Gallery image'; }
    if (cap) cap.textContent = item.caption || '';
    lb.classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeLightbox() {
    const video = document.getElementById('lightbox-video');
    if (video) { video.pause(); video.removeAttribute('src'); video.load(); }
    document.getElementById('lightbox')?.classList.remove('open');
    document.body.style.overflow = '';
}
