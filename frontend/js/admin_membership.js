(() => {
    let board = [], applications = [], reviewingId = null, previewUrl = null;
    const byId = id => document.getElementById(id);
    function node(tag, text, className) {
        const el = document.createElement(tag);
        if (text !== undefined) el.textContent = text;
        if (className) el.className = className;
        return el;
    }
    function button(label, action) {
        const el = node('button', label, 'btn btn-sm btn-outline'); el.type = 'button';
        el.addEventListener('click', action); return el;
    }
    const jsonOptions = (method, body) => ({method, headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
    function setPreview(url) {
        if (previewUrl) { URL.revokeObjectURL(previewUrl); previewUrl = null; }
        const img = byId('board-image-preview');
        img.hidden = !url;
        if (url) img.src = url; else img.removeAttribute('src');
    }
    function boardMessage(message, state = 'info') {
        const el = byId('board-message');
        el.textContent = message;
        el.dataset.state = state;
    }
    function chooseSlot(slot, reveal = false) {
        byId('board-slot').value = slot;
        const member = board.find(m => m.id === Number(slot));
        byId('board-name').value = member?.name || '';
        byId('board-designation').value = member?.designation || '';
        byId('board-image').value = ''; byId('board-remove-image').checked = false;
        setPreview(member?.imageUrl || '');
        byId('board-editor-title').textContent = `Edit board position ${slot}`;
        if (reveal) {
            boardMessage(`Editing position ${slot}. Update the details below, then save.`);
            byId('board-member-form').scrollIntoView({behavior:'smooth', block:'start'});
            byId('board-name').focus({preventScroll:true});
        }
    }
    window.loadBoardAdmin = async () => {
        try {
            board = await apiFetchWithAuth('/api/board-members', {cache:'no-store'});
            if (!board) return;
            const grid = byId('admin-board-grid'); grid.replaceChildren();
            for (let slot=1; slot<=6; slot++) {
                const member = board.find(m => m.id === slot), card = node('div', undefined, 'admin-board-card');
                if (member?.imageUrl?.startsWith('/uploads/board/')) { const img=node('img'); img.src=member.imageUrl; img.alt=member.name; card.append(img); }
                card.append(node('h3', `${slot}. ${member?.name || 'Empty position'}`), node('p', member?.designation || 'Board member'), button('Edit', () => chooseSlot(slot, true)));
                grid.append(card);
            }
            chooseSlot(byId('board-slot').value);
        } catch(e) { byId('board-message').textContent=e.message; }
    };
    function renderApplications() {
        const list=byId('membership-applications'); list.replaceChildren();
        const filter=byId('membership-status-filter').value;
        const matches=applications.filter(m => filter==='ALL' || m.status===filter);
        if (!matches.length) list.append(node('p','No applications in this category.'));
        for (const m of matches) {
            const card=node('article',undefined,'membership-application');
            card.append(node('h3', `${m.name}${m.partnerName ? ' & '+m.partnerName : ''}`),
                node('p', `${m.status} · ${m.membershipType} · ${formatCurrency(m.annualFee)} / year`),
                node('p', `${m.email} · ${formatDateTime(m.appliedAt)}`),
                node('p', `Decision email: ${m.emailDelivery.replaceAll('_',' ').toLowerCase()}`));
            const actions=node('div',undefined,'membership-admin-actions');
            actions.append(button('Review details',()=>openReview(m)));
            if (m.status!=='PENDING' && ['FAILED','PENDING'].includes(m.emailDelivery)) actions.append(button('Retry decision email',async event=>{
                const btn=event.currentTarget; btn.disabled=true;
                try { await apiFetchWithAuth(`/api/admin/members/${m.id}/retry-email`,{method:'POST'}); byId('membership-admin-message').textContent='Email retry requested. Refresh to check delivery status.'; await loadApplications(); }
                catch(e) { byId('membership-admin-message').textContent=e.message; }
                finally { btn.disabled=false; }
            }));
            card.append(actions); list.append(card);
        }
    }
    async function loadApplications() {
        const result=await apiFetchWithAuth('/api/admin/members');
        if (!result) return;
        applications=result; renderApplications();
    }
    window.loadMembershipAdmin = async () => {
        byId('membership-admin-message').textContent='';
        try {
            await loadApplications();
            const s=await apiFetchWithAuth('/api/membership/settings'); if(!s) return;
            byId('membership-benefits-edit').value=s.benefits;
            byId('membership-single-edit').value=s.singleFee;
            byId('membership-couple-edit').value=s.coupleFee;
            byId('membership-payment-edit').value=s.paymentInstructions;
        } catch(e) { byId('membership-admin-message').textContent=e.message; }
    };
    function openReview(m) {
        reviewingId=m.id;
        const details=byId('membership-review-details'); details.replaceChildren();
        const fields={Name:m.name,Partner:m.partnerName,'Partner date of birth':m.partnerDateOfBirth,'Partner address':m.partnerAddress,'Partner phone':m.partnerPhone,'Partner email':m.partnerEmail,'Date of birth':m.dateOfBirth,Address:m.address,Phone:m.phone,Email:m.email,
            Membership:m.membershipType,'Annual fee':formatCurrency(m.annualFee),'Payment method':m.paymentMethod,
            'Applicant reports paid':m.paymentDeclared?'Yes':'No','Payment verified':m.paymentVerified?'Yes':'No',
            'Public name visibility':m.listed?'Show name(s) after approval':'Keep name(s) private',
            'Consent recorded':formatDateTime(m.consentAt),'Consent wording':m.consentText,Status:m.status,'Previous admin note':m.adminNote};
        Object.entries(fields).forEach(([key,value])=> { if(value) { const row=node('div',undefined,'membership-detail'); row.append(node('strong',key),node('span',value)); details.append(row); } });
        byId('membership-decision').value=m.status==='REJECTED'?'REJECTED':'APPROVED';
        byId('membership-payment-verified').checked=m.paymentVerified;
        byId('membership-decision-note').value='';
        byId('membership-review-message').textContent='';
        byId('membership-review-dialog').showModal();
    }
    document.addEventListener('DOMContentLoaded',()=> {
        byId('board-slot').addEventListener('change',e=>chooseSlot(e.target.value, true));
        byId('board-image').addEventListener('change',e=> {
            setPreview(''); const file=e.target.files[0];
            if(file) { previewUrl=URL.createObjectURL(file); byId('board-image-preview').src=previewUrl; byId('board-image-preview').hidden=false; }
        });
        const boardForm = byId('board-member-form');
        boardForm.noValidate = true;
        boardForm.addEventListener('submit',async e=> {
            e.preventDefault();
            if (!boardForm.reportValidity()) {
                boardMessage('Please complete the required name and designation and check your selected photo.', 'error');
                return;
            }
            const btn=e.currentTarget.querySelector('[type=submit]'); setLoading(btn,true);
            boardMessage('Saving board member…');
            try {
                const data=new FormData(); data.append('name',byId('board-name').value.trim()); data.append('designation',byId('board-designation').value.trim());
                data.append('removeImage',byId('board-remove-image').checked);
                if(byId('board-image').files[0]) data.append('image',byId('board-image').files[0]);
                const saved=await apiFetchWithAuth(`/api/admin/board-members/${byId('board-slot').value}`,{method:'PUT',body:data});
                if(!saved?.id) throw new Error('The server did not confirm the save. Please sign in again if your session expired, then retry.');
                await loadBoardAdmin();
                boardMessage('Board member saved successfully. Refresh About Us to see the updated details.', 'success');
            } catch(err) { boardMessage(err.message || 'Unable to save the board member. Please try again.', 'error'); }
            finally { setLoading(btn,false); }
        });
        byId('board-clear').addEventListener('click',async e=> {
            if(!confirm('Clear this board position? It will show as “To be announced”.')) return;
            e.currentTarget.disabled=true;
            try { await apiFetchWithAuth(`/api/admin/board-members/${byId('board-slot').value}`,{method:'DELETE'}); await loadBoardAdmin(); byId('board-message').textContent='Position cleared.'; }
            catch(err) { byId('board-message').textContent=err.message; }
            finally { byId('board-clear').disabled=false; }
        });
        byId('members-refresh').addEventListener('click',()=>loadApplications().catch(e=>byId('membership-admin-message').textContent=e.message));
        byId('membership-status-filter').addEventListener('change',renderApplications);
        byId('membership-review-close').addEventListener('click',()=>byId('membership-review-dialog').close());
        byId('membership-review-form').addEventListener('submit',async e=> {
            e.preventDefault();
            if (byId('membership-decision').value === 'APPROVED' && !byId('membership-payment-verified').checked) {
                byId('membership-review-message').textContent = 'Approval has not been saved. Verify the payment, tick the payment checkbox, then save again.';
                byId('membership-payment-verified').focus();
                return;
            }
            const btn=e.currentTarget.querySelector('[type=submit]'); setLoading(btn,true);
            byId('membership-review-message').textContent='Saving decision…';
            try {
                const saved=await apiFetchWithAuth(`/api/admin/members/${reviewingId}/decision`,jsonOptions('PUT',{
                    status:byId('membership-decision').value,paymentVerified:byId('membership-payment-verified').checked,adminNote:byId('membership-decision-note').value.trim()
                }));
                if(!saved?.id) throw new Error('The server did not confirm this decision. Please sign in again and retry.');
                byId('membership-review-dialog').close(); await loadApplications();
                byId('membership-admin-message').textContent='Decision saved. Refresh to check email delivery status. Re-saving the same decision does not send another email.';
            } catch(err) { byId('membership-review-message').textContent=err.message; }
            finally { setLoading(btn,false); }
        });
        byId('membership-settings-form').addEventListener('submit',async e=> {
            e.preventDefault(); const btn=e.currentTarget.querySelector('[type=submit]'); setLoading(btn,true);
            try {
                const saved=await apiFetchWithAuth('/api/admin/membership/settings',jsonOptions('PUT',{
                    benefits:byId('membership-benefits-edit').value.trim(),singleFee:Number(byId('membership-single-edit').value),
                    coupleFee:Number(byId('membership-couple-edit').value),paymentInstructions:byId('membership-payment-edit').value.trim()
                }));
                if(saved) byId('membership-settings-admin-message').textContent='Membership information saved.';
            } catch(err) { byId('membership-settings-admin-message').textContent=err.message; }
            finally { setLoading(btn,false); }
        });
    });
})();
