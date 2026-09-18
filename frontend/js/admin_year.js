let adminSelectedYear=null, adminSelectedEdition=null, adminEditions=[], adminYearsPromise;
function updateManagedYearLabels(){
 const current=adminEditions.find(e=>e.eventEditionId===adminSelectedEdition);
 adminSelectedYear=current?.eventYear ?? null;
 document.querySelectorAll('[data-managed-year]').forEach(el=>el.textContent=adminSelectedYear??'');
 document.querySelectorAll('[data-managed-title]').forEach(el=>el.textContent=current?.title??'');
}
async function ensureAdminYears(refresh=false){
 if(!refresh&&adminYearsPromise)return adminYearsPromise;
 adminYearsPromise=(async()=>{
 const [active,editions]=await Promise.all([getActiveEventConfig(refresh),apiFetchWithAuth('/api/admin/editions',{cache:'no-store'})]);
 if(!Array.isArray(editions))throw Error('Please sign in again to load events.');
 adminEditions=editions;
 if(!editions.some(e=>e.eventEditionId===adminSelectedEdition))adminSelectedEdition=active.eventEditionId;
 for(const id of ['admin-year-filter','active-event-year']){
  const select=document.getElementById(id);select.replaceChildren();
  for(const e of editions){const option=contentNode('option',e.title+(e.eventEditionId===active.eventEditionId?' (active)':''));option.value=e.eventEditionId;select.append(option);}
  select.value=id==='admin-year-filter'?adminSelectedEdition:active.eventEditionId;
 }
 document.getElementById('active-year-current').textContent=active.title;
 updateManagedYearLabels();return adminSelectedEdition;
 })().catch(e=>{adminYearsPromise=null;throw e;});return adminYearsPromise;
}
function adminYearQuery(){return adminSelectedEdition===null?'':'?eventEditionId='+adminSelectedEdition;}
async function refreshManagedEvent(){await Promise.all([loadDashboardStats(),loadRegistrations(),loadPerformers(),loadEvents(),loadConfig(),loadGalleryYears()]);}
document.addEventListener('DOMContentLoaded',()=>{
 document.getElementById('admin-year-filter').addEventListener('change',async e=>{
  adminSelectedEdition=Number(e.target.value);updateManagedYearLabels();
  try{await refreshManagedEvent();}catch(err){showAlert('config-alert',err.message,'error');}
 });
 document.getElementById('activate-event-year').addEventListener('click',async e=>{
  const id=Number(document.getElementById('active-event-year').value),edition=adminEditions.find(e=>e.eventEditionId===id);
  if(!edition||!confirm('Make '+edition.title+' the public event? Visitors will see its theme and registration details.'))return;
  const button=e.currentTarget,message=document.getElementById('active-year-message');button.disabled=true;message.textContent='Activating event…';
  try{const saved=await apiFetchWithAuth('/api/admin/editions/'+id+'/activate',{method:'PUT'});if(!saved?.eventEditionId)throw Error('Activation was not confirmed. Sign in again.');
   adminSelectedEdition=id;await ensureAdminYears(true);await refreshManagedEvent();message.textContent=saved.title+' is now active.';
  }catch(err){message.textContent=err.message;}finally{button.disabled=false;}
 });
});
