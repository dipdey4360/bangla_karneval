let programmeEditor=null, programmeChoices=null, programmeEditorRequest=0;
const programmeFieldMap={title:'title',tagline:'tagline',about:'aboutText',date:'eventDate',location:'eventLocation',price:'pricePerPerson',theme:'themeKey',accent:'accentColor',payment:'paymentInstructions',phone:'contactPhone',email:'contactEmail',whatsapp:'contactWhatsapp',facebook:'contactFacebook',instagram:'contactInstagram'};
const programmeColours={'bangla-karneval':'#7b241c',eid:'#146b52',puja:'#a92330','bangla-noboborsho':'#b42c35',bbq:'#a34d16',game:'#245bb3'};
async function loadProgrammeChoices(){
 if(!programmeChoices)programmeChoices=await apiFetchWithAuth('/api/admin/programmes');
 if(!Array.isArray(programmeChoices))throw Error('Sign in again to load programmes.');
 for(const id of ['cfg-programme','cfg-theme']){const s=document.getElementById(id);if(!s.options.length)for(const p of programmeChoices){const o=contentNode('option',p.name);o.value=p.code;s.append(o);}}
}
function populateProgrammeEditor(c){
 programmeEditor=c;document.getElementById('programme-editor-heading').textContent=c.eventEditionId?'Edit '+c.title:'Create an event';
 document.getElementById('cfg-programme').value=c.programmeCode;document.getElementById('cfg-year').value=c.eventYear;
 document.getElementById('cfg-programme').disabled=!!c.eventEditionId;document.getElementById('cfg-year').disabled=!!c.eventEditionId;
 for(const [field,property] of Object.entries(programmeFieldMap))document.getElementById('cfg-'+field).value=c[property]??'';
 document.getElementById('cfg-registration').checked=!!c.registrationEnabled;document.getElementById('cfg-performer').checked=!!c.performerEnabled;
 const preview=document.getElementById('preview-programme');preview.hidden=!c.eventEditionId;preview.href=c.eventEditionId?'event_preview.html?edition='+c.eventEditionId:'#';
 showEventPoster(document.getElementById('programme-poster-preview'),c);
 document.getElementById('programme-poster-form').querySelector('button').disabled=!c.eventEditionId;
 document.getElementById('programme-poster').value='';
}
async function loadProgrammeEditor(){
 const seq=++programmeEditorRequest;
 try{await ensureAdminYears();await loadProgrammeChoices();const id=adminSelectedEdition;
 const c=await apiFetchWithAuth('/api/admin/editions/'+id);if(seq!==programmeEditorRequest||id!==adminSelectedEdition)return;
 if(!c?.eventEditionId)throw Error('Sign in again to load event settings.');populateProgrammeEditor(c);
 }catch(e){showAlert('config-alert',e.message,'error');}
}
async function loadOrganisationEditor(){try{const c=await apiFetch('/api/organisation');for(const [field,key] of Object.entries({story:'story',phone:'contactPhone',email:'contactEmail',facebook:'contactFacebook',instagram:'contactInstagram'}))document.getElementById('org-'+field).value=c[key]||'';}catch(e){document.getElementById('organisation-message').textContent=e.message;}}
document.addEventListener('DOMContentLoaded',()=>{
 loadOrganisationEditor();
 document.getElementById('new-programme-event').addEventListener('click',async()=>{
  await loadProgrammeChoices();++programmeEditorRequest;
  populateProgrammeEditor({programmeCode:'bangla-karneval',eventYear:new Date().getFullYear(),title:'Bangla Karneval '+new Date().getFullYear(),themeKey:'bangla-karneval',accentColor:'#7b241c',pricePerPerson:10,tagline:'Celebrating culture and community',registrationEnabled:false,performerEnabled:false});
  document.getElementById('config-alert').textContent='New event: fill in the details and save.';document.getElementById('cfg-programme').focus();
 });
 const newDefaults=()=>{if(programmeEditor?.eventEditionId)return;const code=document.getElementById('cfg-programme').value;document.getElementById('cfg-title').value=programmeChoices.find(p=>p.code===code).name+' '+document.getElementById('cfg-year').value;document.getElementById('cfg-theme').value=code;document.getElementById('cfg-accent').value=programmeColours[code];document.getElementById('cfg-tagline').value=eventTagline({programmeCode:code});};
 const refreshPoster=()=>showEventPoster(document.getElementById('programme-poster-preview'),{...programmeEditor,title:document.getElementById('cfg-title').value});
 document.getElementById('cfg-title').addEventListener('input',refreshPoster);
 document.getElementById('cfg-theme').addEventListener('change',e=>{
  document.getElementById('cfg-accent').value=programmeColours[e.target.value] || '#a82c37';
 });
 document.getElementById('cfg-programme').addEventListener('change',()=>{newDefaults();refreshPoster();});document.getElementById('cfg-year').addEventListener('change',()=>{newDefaults();refreshPoster();});
 document.getElementById('programme-form').addEventListener('submit',async e=>{
  e.preventDefault();if(programmeEditor?.eventEditionId && programmeEditor.eventEditionId!==adminSelectedEdition){showAlert('config-alert','Wait for the selected event to load before saving.','error');return;}if(!programmeEditor){showAlert('config-alert','Load or create an event first.','error');return;}
  const button=e.target.querySelector('[type=submit]');button.disabled=true;
  const id=programmeEditor.eventEditionId,request={programmeCode:document.getElementById('cfg-programme').value,eventYear:Number(document.getElementById('cfg-year').value),version:programmeEditor.version??null};
  for(const [field,key] of Object.entries(programmeFieldMap))request[key]=document.getElementById('cfg-'+field).value.trim();
  request.pricePerPerson=Number(request.pricePerPerson);request.eventDate=request.eventDate||null;
  request.registrationEnabled=document.getElementById('cfg-registration').checked;request.performerEnabled=document.getElementById('cfg-performer').checked;
  try{const saved=await apiFetchWithAuth('/api/admin/editions'+(id?'/'+id:''),{method:id?'PUT':'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(request)});
   if(!saved?.eventEditionId)throw Error('Save was not confirmed. Sign in again.');
   adminSelectedEdition=saved.eventEditionId;await ensureAdminYears(true);populateProgrammeEditor(saved);showAlert('config-alert','Event saved. You can now preview it, upload a poster, or activate it.','success');
  }catch(err){showAlert('config-alert',err.message,'error');}finally{button.disabled=false;}
 });
 document.getElementById('programme-poster-form').addEventListener('submit',async e=>{
  e.preventDefault();if(!programmeEditor?.eventEditionId || programmeEditor.eventEditionId!==adminSelectedEdition)return;const id=programmeEditor.eventEditionId,button=e.target.querySelector('button'),message=document.getElementById('programme-poster-message');button.disabled=true;
  try{const data=new FormData();data.append('image',document.getElementById('programme-poster').files[0]);const saved=await apiFetchWithAuth('/api/admin/editions/'+id+'/poster',{method:'POST',body:data});if(!saved?.eventEditionId)throw Error('Upload was not confirmed.');
   if(programmeEditor?.eventEditionId===id){programmeEditor.version=saved.version;programmeEditor.posterPath=saved.posterPath;showEventPoster(document.getElementById('programme-poster-preview'),saved);}
   message.textContent='Poster uploaded successfully.';
  }catch(err){message.textContent=err.message;}finally{button.disabled=false;}
 });
 document.getElementById('organisation-form').addEventListener('submit',async e=>{
  e.preventDefault();const button=e.target.querySelector('button'),message=document.getElementById('organisation-message');button.disabled=true;
  const request={};for(const [field,key] of Object.entries({story:'story',phone:'contactPhone',email:'contactEmail',facebook:'contactFacebook',instagram:'contactInstagram'}))request[key]=document.getElementById('org-'+field).value.trim();
  try{const saved=await apiFetchWithAuth('/api/admin/organisation',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(request)});if(!saved?.name)throw Error('Save was not confirmed.');message.textContent='Organisation details saved. These are shared across all programmes.';}catch(err){message.textContent=err.message;}finally{button.disabled=false;}
 });
});
