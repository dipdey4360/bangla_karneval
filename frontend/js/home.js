async function loadEventCards(selectedConfig) {
    const request = ++homeActivityRequest;
    const grid = document.getElementById('event-cards');
    if (!grid) return;

    try {
        const config = selectedConfig || await getActiveEventConfig();
        const events = await apiFetch(`/api/editions/${config.eventEditionId}/activities`);
        if (request !== homeActivityRequest) return;
        grid.innerHTML = '';

        if (!events || events.length === 0) {
            grid.innerHTML = '<p class="no-events">Programme details will be announced soon.</p>';
            return;
        }

        events.forEach(ev => {
            const card = document.createElement('div');
            card.className = 'event-card';
            const icon = contentNode('div', null, 'event-icon');
            const image = contentNode('img');
            image.src = safeWebUrl(ev.iconUrl) || '/assets/images/default-event.png';
            image.alt = ev.title || '';
            image.addEventListener('error', () => { image.style.display = 'none'; });
            icon.append(image);
            card.append(icon, contentNode('h3', ev.title), contentNode('p', ev.description || ''));
            grid.appendChild(card);
        });

        grid.dataset.count = events.length;

    } catch (e) {
        if (request !== homeActivityRequest) return;
        console.error('Failed to load event cards:', e);
        grid.innerHTML = '<p class="no-events">Unable to load the programme. Please refresh to try again.</p>';
    }
}

let homeEvents = [], homeSelected = 0, homeActiveId = null, homePreview = false, homeActivityRequest = 0;

function orderHomeEvents(events, active) {
    const unique = new Map(events.map(event => [event.eventEditionId, event]));
    unique.set(active.eventEditionId, active);
    const anchor = active.eventDate || `${active.eventYear}-01-01`;
    const others = [...unique.values()].filter(event => event.eventEditionId !== active.eventEditionId);
    const earlier = event => event.eventDate ? event.eventDate < anchor : event.eventYear < active.eventYear;
    const byDate = (a,b) => (a.eventDate || `${a.eventYear}-12-31`).localeCompare(b.eventDate || `${b.eventYear}-12-31`) || a.eventEditionId-b.eventEditionId;
    return [...others.filter(earlier).sort(byDate),active,...others.filter(event=>!earlier(event)).sort(byDate)];
}
function homeEventStatus(event) {
    if (event.eventEditionId === homeActiveId) return homePreview ? 'Event preview' : 'Current event';
    const today = new Intl.DateTimeFormat('en-CA',{timeZone:'Europe/Berlin',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
    if (event.eventDate) return event.eventDate < today ? 'Past event' : 'Upcoming event';
    return event.eventYear < Number(today.slice(0,4)) ? 'Past event' : 'Date to be announced';
}
async function loadHomeEvents() {
    const stage=document.getElementById('event-carousel');
    if(!stage) return;
    try {
        homePreview=location.pathname.endsWith('/event_preview.html');
        const active=await getActiveEventConfig();
        if(!active?.eventEditionId) throw Error('No current event');
        homeActiveId=active.eventEditionId;
        let events=[];
        try { events=homePreview?[active]:await apiFetch('/api/editions/home',{cache:'no-store'}); }
        catch(error) { console.warn('Event catalogue unavailable; showing current event',error); }
        homeEvents=orderHomeEvents(Array.isArray(events)?events:[],active);
        homeSelected=homeEvents.findIndex(event=>event.eventEditionId===homeActiveId);
        stage.replaceChildren();
        const selectors=document.getElementById('event-selector');selectors.replaceChildren();
        homeEvents.forEach((event,index)=>{
            const slide=contentNode('article',null,'event-slide');slide.setAttribute('role','group');slide.setAttribute('aria-roledescription','slide');
            slide.setAttribute('aria-label',`${index+1} of ${homeEvents.length}: ${event.title}`);
            const fallback=contentNode('div',event.title,'event-artwork-fallback');
            const image=contentNode('img');image.alt=event.title+' poster';image.decoding='async';image.loading=index===homeSelected?'eager':'lazy';
            const url=eventPosterUrl(event);fallback.hidden=!!url;
            if(url){image.src=url;image.addEventListener('error',()=>{image.hidden=true;fallback.hidden=false;});}else image.hidden=true;
            const label=contentNode('div',null,'event-slide-label');label.append(contentNode('small',homeEventStatus(event).toUpperCase()),contentNode('strong',event.title));
            slide.append(fallback,image,label);stage.append(slide);
            const button=contentNode('button',event.title);button.type='button';button.addEventListener('click',()=>selectHomeEvent(index));selectors.append(button);
        });
        document.getElementById('event-previous').onclick=()=>selectHomeEvent(homeSelected-1);
        document.getElementById('event-next').onclick=()=>selectHomeEvent(homeSelected+1);
        let startX=null;
        stage.addEventListener('touchstart',event=>{startX=event.changedTouches[0].clientX;},{passive:true});
        stage.addEventListener('touchend',event=>{if(startX!==null){const distance=event.changedTouches[0].clientX-startX;if(Math.abs(distance)>50)selectHomeEvent(homeSelected+(distance<0?1:-1));startX=null;}},{passive:true});
        selectHomeEvent(homeSelected);
    } catch(error) {
        stage.replaceChildren(contentNode('p','Event details are temporarily unavailable. Please refresh to try again.','carousel-loading'));
        document.getElementById('hero-register-btn').hidden=true;
    }
}
function selectHomeEvent(index) {
    if(index<0 || index>=homeEvents.length) return;
    homeSelected=index;
    const event=homeEvents[index];
    document.querySelectorAll('.event-slide').forEach((slide,i)=>{
        const offset=i-index;
        slide.style.transform=`translateX(${offset*93}%) scale(${offset===0?1:.87})`;
        slide.style.opacity=offset===0?'1':'.42';slide.style.zIndex=offset===0?'3':'1';
        slide.style.visibility=Math.abs(offset)>1?'hidden':'visible';slide.setAttribute('aria-hidden',String(offset!==0));
    });
    document.querySelectorAll('#event-selector button').forEach((button,i)=>button.setAttribute('aria-pressed',String(i===index)));
    document.getElementById('event-previous').disabled=index===0;
    document.getElementById('event-next').disabled=index===homeEvents.length-1;
    document.getElementById('carousel-heading').textContent=homeEventStatus(event);
    document.getElementById('selected-event-title').textContent=event.title;
    document.getElementById('selected-event-tagline').textContent=eventTagline(event);
    document.getElementById('event-date').textContent=event.eventDate?formatDate(event.eventDate):'Date to be announced';
    document.getElementById('event-venue').textContent=event.eventLocation || 'Venue to be announced';
    const canRegister=!homePreview && event.eventEditionId===homeActiveId && event.registrationEnabled===true;
    document.getElementById('hero-register-btn').hidden=!canRegister;
    document.getElementById('hero-register-btn').href='/registration.html?edition='+encodeURIComponent(event.eventEditionId);
    document.getElementById('event-registration-status').textContent=canRegister?'':homePreview?'Registration is unavailable in preview.':homeEventStatus(event)==='Past event'?'Registration is closed for this event.':event.eventEditionId===homeActiveId?'Registration is currently closed.':'Registration is not open for this event.';
    document.getElementById('event-description').textContent=event.aboutText || `Discover what is planned for ${event.title}.`;
    loadEventCards(event);
}
document.addEventListener('DOMContentLoaded',loadHomeEvents);
