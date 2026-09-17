let adminSelectedYear = null;
let adminYearsPromise;
function updateManagedYearLabels() {
    document.querySelectorAll('[data-managed-year]').forEach(node => node.textContent = adminSelectedYear ?? '');
}
async function ensureAdminYears(refresh = false) {
    if (!refresh && adminYearsPromise) return adminYearsPromise;
    adminYearsPromise = (async () => {
        const [config, years] = await Promise.all([
            getActiveEventConfig(refresh), apiFetchWithAuth('/api/admin/gallery/years', {cache:'no-store'})
        ]);
        if (!Array.isArray(years)) throw new Error('Please sign in again to load event years.');
        const values = [...new Set(years.map(Number))].sort((a,b)=>b-a);
        if (!values.includes(Number(config.eventYear))) values.unshift(Number(config.eventYear));
        if (adminSelectedYear === null || !values.includes(adminSelectedYear)) adminSelectedYear = Number(config.eventYear);
        for (const id of ['admin-year-filter','active-event-year']) {
            const select = document.getElementById(id); select.replaceChildren();
            values.forEach(year => {
                const option = contentNode('option', `${year}${year === Number(config.eventYear) ? ' (active)' : ''}`);
                option.value = year; select.append(option);
            });
            select.value = id === 'admin-year-filter' ? adminSelectedYear : config.eventYear;
        }
        document.getElementById('active-year-current').textContent = config.eventYear;
        updateManagedYearLabels();
        await updateEventYearLabels();
        return adminSelectedYear;
    })().catch(error => { adminYearsPromise=null; throw error; });
    return adminYearsPromise;
}
function adminYearQuery() { return adminSelectedYear === null ? '' : `?year=${adminSelectedYear}`; }
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('admin-year-filter').addEventListener('change', async event => {
        adminSelectedYear = Number(event.target.value);
        updateManagedYearLabels();
        await Promise.all([loadDashboardStats(), loadRegistrations(), loadPerformers(), loadEvents(), loadConfig()]);
    });
    document.getElementById('activate-event-year').addEventListener('click', async event => {
        const button = event.currentTarget, message = document.getElementById('active-year-message');
        button.disabled = true; message.textContent = 'Activating event year…';
        try {
            const year = Number(document.getElementById('active-event-year').value);
            const saved = await apiFetchWithAuth('/api/admin/config/active-year', {
                method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({eventYear:year})
            });
            if (!saved?.eventYear) throw new Error('The year change was not confirmed. Sign in again and retry.');
            adminSelectedYear = Number(saved.eventYear);
            await ensureAdminYears(true);
            await Promise.all([loadConfig(),loadDashboardStats(),loadRegistrations(),loadPerformers(),loadEvents(),loadButtonVisibility(),loadGalleryYears()]);
            message.textContent = `${saved.eventYear} is now active. Existing registrations and membership settings are unchanged.`;
        } catch (error) { message.textContent = error.message; }
        finally { button.disabled = false; }
    });
});
