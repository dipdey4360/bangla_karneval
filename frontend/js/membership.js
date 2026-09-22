(() => {
    let settings;
    const byId = id => document.getElementById(id);
    function element(tag, text, className) {
        const node = document.createElement(tag);
        if (text !== undefined) node.textContent = text;
        if (className) node.className = className;
        return node;
    }
    async function loadBoard() {
        const grid = byId('board-grid');
        try {
            const members = await apiFetch('/api/board-members', {cache:'no-store'});
            grid.replaceChildren();
            for (let slot = 1; slot <= 6; slot++) {
                const member = members.find(m => m.id === slot);
                const card = element('article', undefined, 'board-card');
                const photo = element('div', undefined, 'board-photo');
                const placeholder = element('span', 'BK', 'board-placeholder');
                placeholder.setAttribute('aria-hidden', 'true');
                photo.append(placeholder);
                if (member?.imageUrl?.startsWith('/uploads/board/')) {
                    const image = element('img');
                    image.src = member.imageUrl;
                    image.alt = member.name;
                    image.loading = 'lazy';
                    image.addEventListener('error', () => image.remove());
                    photo.append(image);
                }
                const details = element('div', undefined, 'board-details');
                details.append(element('h3', member?.name || 'To be announced'), element('p', member?.designation || 'Board member'));
                card.append(photo, details); grid.append(card);
            }
        } catch (e) { grid.textContent = 'Board members could not be loaded. Please refresh to try again.'; }
    }
    async function loadMembers() {
        const grid = byId('members-grid');
        try {
            const members = await apiFetch('/api/members', {cache:'no-store'});
            grid.replaceChildren();
            const demoNames = ['Arif Rahman', 'Nadia Ahmed', 'Tanvir Hasan', 'Farhana Islam', 'Rafiq Chowdhury', 'Samira Akter', 'Imran Hossain', 'Nusrat Jahan', 'Sajid Karim', 'Mithila Roy'];
            if (!members.length) {
                grid.append(element('p', 'Sample names are shown until approved members are available.', 'membership-empty'));
            }
            const displayedMembers = members.length ? members : demoNames.map(name => ({name}));
            displayedMembers.forEach(m => grid.append(element('div', m.name, 'member-name-card')));
        } catch (e) { grid.textContent = 'Members could not be loaded. Please refresh to try again.'; }
    }
    function updateFee() {
        const couple = byId('m-type').value === 'COUPLE';
        byId('m-partner-group').hidden = !couple;
        byId('m-partner-group').disabled = !couple;
        if (!settings) return;
        byId('m-fee').textContent = formatCurrency(couple ? settings.coupleFee : settings.singleFee);
    }
    async function loadSettings() {
        try {
            settings = await apiFetch('/api/membership/settings');
            byId('single-fee').textContent = formatCurrency(settings.singleFee);
            byId('couple-fee').textContent = formatCurrency(settings.coupleFee);
            const list = byId('membership-benefits-list'); list.replaceChildren();
            settings.benefits.split('\n').filter(line => line.trim()).forEach(line => list.append(element('li', line.trim())));
            byId('m-payment-instructions').textContent = settings.paymentInstructions;
            byId('membership-apply').disabled = false;
            byId('membership-settings-message').textContent = '';
            updateFee();
        } catch (e) { byId('membership-settings-message').textContent = 'Membership details could not be loaded. Please refresh before applying.'; }
    }
    document.addEventListener('DOMContentLoaded', () => {
        loadBoard(); loadMembers(); loadSettings();
        const dialog = byId('membership-dialog');
        const yesterday = new Date(); yesterday.setDate(yesterday.getDate() - 1);
        byId('m-dob').max = `${yesterday.getFullYear()}-${String(yesterday.getMonth()+1).padStart(2,'0')}-${String(yesterday.getDate()).padStart(2,'0')}`;
        byId('m-partner-dob').max = byId('m-dob').max;
        byId('membership-apply').addEventListener('click', () => {
            byId('membership-form-message').textContent = '';
            dialog.showModal();
        });
        byId('membership-close').addEventListener('click', () => dialog.close());
        byId('m-type').addEventListener('change', updateFee);
        byId('membership-form').addEventListener('submit', async event => {
            event.preventDefault();
            const form = event.currentTarget;
            if (!form.reportValidity()) return;
            const btn = byId('membership-submit'); setLoading(btn, true);
            const message = byId('membership-form-message'); message.textContent = '';
            const payload = {
                name: byId('m-name').value.trim(),
                dateOfBirth: byId('m-dob').value, address: byId('m-address').value.trim(),
                phone: byId('m-phone').value.trim(), email: byId('m-email').value.trim(),
                membershipType: byId('m-type').value, paymentMethod: byId('m-payment').value,
                paymentDeclared: byId('m-paid').checked, consent: byId('m-consent').checked,
                listed: byId('m-listed').value === 'true'
            };
            if (payload.membershipType === 'COUPLE') {
                payload.partnerName = byId('m-partner').value.trim();
                payload.partnerDateOfBirth = byId('m-partner-dob').value;
                payload.partnerAddress = byId('m-partner-address').value.trim();
                payload.partnerPhone = byId('m-partner-phone').value.trim();
                payload.partnerEmail = byId('m-partner-email').value.trim();
            }
            try {
                const result = await apiFetch('/api/membership/applications', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload)});
                form.reset(); updateFee(); dialog.close();
                byId('membership-settings-message').textContent = result.message;
                byId('membership-settings-message').scrollIntoView({behavior:'smooth', block:'center'});
            } catch (e) { message.textContent = e.message || 'Submission failed. Please try again.'; }
            finally { setLoading(btn, false); }
        });
    });
})();
