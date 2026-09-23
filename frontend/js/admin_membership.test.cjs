const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

test('member deletion confirms both names, preserves cancelled/failed records and removes successful records', async () => {
    const elements = new Map();
    const make = () => ({value:'ALL', children:[], listeners:{},
        addEventListener(name, fn){this.listeners[name]=fn;}, append(...items){this.children.push(...items);},
        replaceChildren(){this.children=[];}});
    const get = id => {if (!elements.has(id)) elements.set(id, make()); return elements.get(id);};
    let confirmed = false, failure = false, deleted = 0, prompt;
    const member = {id:7,name:'Alice',partnerName:'Bob',membershipType:'COUPLE',status:'APPROVED',emailDelivery:'SENT'};
    const context = {document:{getElementById:get,createElement:make,addEventListener(){}},
        formatCurrency:()=>'',formatDateTime:()=>'',
        confirm:text=>{prompt=text; return confirmed;},
        apiFetchWithAuth:async(url, options={})=>{
            if(options.method==='DELETE') {
                assert.equal(url, '/api/admin/members/7');
                if(failure) throw Error('Deletion failed');
                deleted++; return null;
            }
            return url==='/api/admin/members' ? [member] : {};
        }};
    context.window=context;
    vm.runInNewContext(fs.readFileSync(require('node:path').join(__dirname,'admin_membership.js'),'utf8'),context);
    await context.loadMembershipAdmin();
    const card=get('membership-applications').children[0];
    const btn=card.children.at(-1).children.find(child=>child.textContent==='Delete member');
    await btn.listeners.click({currentTarget:btn});
    assert.match(prompt,/Alice & Bob/); assert.match(prompt,/Both partners/); assert.equal(deleted,0);
    confirmed=true; failure=true;
    await btn.listeners.click({currentTarget:btn});
    assert.equal(get('membership-applications').children[0],card);
    assert.equal(get('membership-admin-message').textContent,'Deletion failed'); assert.equal(btn.disabled,false);
    failure=false;
    await btn.listeners.click({currentTarget:btn});
    assert.equal(deleted,1);
    assert.match(get('membership-applications').children[0].textContent,/No applications/);
    assert.match(get('membership-admin-message').textContent,/deleted successfully/);
});

test('board editor selects a card, saves multipart data, refreshes cards and reports failures', async () => {
    const elements = new Map();
    const make = () => ({value:'', checked:false, files:[], dataset:{}, children:[], listeners:{},
        addEventListener(name, fn){this.listeners[name]=fn;}, append(...children){this.children.push(...children);},
        replaceChildren(){this.children=[];}, removeAttribute(){}, scrollIntoView(){this.scrolled=true;},
        focus(){this.focused=true;}, reportValidity(){return true;}, querySelector(){return make();}});
    const html=fs.readFileSync(require('node:path').join(__dirname,'../admin_dashboard.html'),'utf8');
    for(const match of html.matchAll(/id="([^"]+)"/g)) elements.set(match[1],make());
    const get=id=>{assert.ok(elements.has(id), `Missing element ${id}`); return elements.get(id);};
    get('board-slot').value='1';
    let initialize, saved, fail=false, members=[{id:2,name:'Original',designation:'Secretary'}];
    const context={document:{getElementById:get,createElement:make,addEventListener:(name,fn)=>initialize=fn},
        FormData,URL,console,setLoading(){},apiFetchWithAuth:async(url,options={})=>{
            if(options.method==='PUT') {
                if(fail) throw Error('Save failed in test');
                assert.equal(url,'/api/admin/board-members/2');
                saved={id:2,name:options.body.get('name'),designation:options.body.get('designation')};
                members=[saved]; return saved;
            }
            return members;
        }};
    context.window=context;
    vm.runInNewContext(fs.readFileSync(require('node:path').join(__dirname,'admin_membership.js'),'utf8'),context);
    initialize(); await context.loadBoardAdmin();
    get('admin-board-grid').children[1].children[2].listeners.click();
    assert.equal(get('board-name').value,'Original');
    assert.ok(get('board-member-form').scrolled);
    assert.ok(get('board-name').focused);
    get('board-name').value='Updated Name';
    const event={preventDefault(){},currentTarget:get('board-member-form')};
    await event.currentTarget.listeners.submit(event);
    assert.equal(saved.name,'Updated Name');
    assert.equal(get('board-message').dataset.state,'success');
    assert.match(get('admin-board-grid').children[1].children[0].textContent,/Updated Name/);
    fail=true;
    await event.currentTarget.listeners.submit(event);
    assert.equal(get('board-message').dataset.state,'error');
    assert.equal(get('board-message').textContent,'Save failed in test');
});

test('membership review displays expiry, reminder dates and delivery status', async () => {
    const elements = new Map();
    const make = () => ({value:'ALL',children:[],listeners:{},addEventListener(name,fn){this.listeners[name]=fn;},
        append(...children){this.children.push(...children);},replaceChildren(){this.children=[];},showModal(){this.open=true;}});
    const get=id=>{if(!elements.has(id)) elements.set(id,make());return elements.get(id);};
    const member={id:9,name:'Alice',partnerName:'Bob',membershipType:'COUPLE',status:'APPROVED',emailDelivery:'SENT',
        validityStatus:'Active',membershipId:'BKM-00009',membershipStartsOn:'2026-09-23',membershipExpiresOn:'2027-09-23',
        expiryReminderDueOn:'2027-08-23',expiryReminderSentAt:'2027-08-23T09:00:00',partnerExpiryReminderSentAt:null};
    const context={document:{getElementById:get,createElement:make,addEventListener(){}},
        formatCurrency:()=>'',formatDateTime:value=>value || '',apiFetchWithAuth:async url=>url==='/api/admin/members'?[member]:{}};
    context.window=context;
    vm.runInNewContext(fs.readFileSync(require('node:path').join(__dirname,'admin_membership.js'),'utf8'),context);
    await context.loadMembershipAdmin();
    const card=get('membership-applications').children[0];
    assert.ok(card.children.some(child=>child.textContent?.includes('Expires: 2027-09-23')));
    card.children.at(-1).children.find(child=>child.textContent==='Review details').listeners.click();
    const details=Object.fromEntries(get('membership-review-details').children.map(row=>row.children.map(child=>child.textContent)));
    assert.equal(details['Membership starts'],'2026-09-23');
    assert.equal(details['Membership expires'],'2027-09-23');
    assert.equal(details['Expiry reminder due'],'2027-08-23');
    assert.equal(details['Expiry reminder sent'],'2027-08-23T09:00:00');
    assert.equal(details['Partner expiry reminder sent'],'Not sent');
    assert.equal(get('membership-review-dialog').open,true);
});
