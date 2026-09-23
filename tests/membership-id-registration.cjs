const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
function node(value='') {
    return {value, textContent:'', hidden:false, disabled:false, listeners:{}, classList:{add(){}},
        addEventListener(type,fn){this.listeners[type]=fn;},
        set innerHTML(html){this.html=html; this.parts = Object.fromEntries(['member-choice','member-id','member-details','member-verify','member-status'].map(key=>['.'+key,node(key==='member-choice'?'no':'')]));},
        get innerHTML(){return this.html;}, querySelector(key){return this.parts?.[key];}};
}
const elements = new Map();
for (const key of ['primary-membership','primary-name','price-summary-body','total-amount','primary-price-display']) elements.set(key,node());
const hosts = [elements.get('primary-membership')], cards=[];
let resolveRequest, rejectRequest, sent;
const context = {console, document:{addEventListener(){},getElementById:id=>elements.get(id),
    querySelectorAll:selector=>selector==='.membership-check'?hosts:cards},
    formatCurrency:v=>Number(v).toFixed(2),apiFetch:async(url,options)=>{sent=JSON.parse(options.body); return new Promise((resolve,reject)=>{resolveRequest=resolve;rejectRequest=reject;});}};
vm.createContext(context);
vm.runInContext(fs.readFileSync('frontend/js/registration.js','utf8'),context);
const primary = hosts[0], name=elements.get('primary-name');
context.mountMembershipCheck(primary,name);
function selectYes(host, person, id='BKM-00001') {
    const state=host.membershipState; state.nameInput.value=person; state.choice.value='yes'; state.input.value=id;
    state.choice.listeners.change();
}
(async()=>{
    selectYes(primary,'Alice');
    assert.equal(context.membershipsReady(),false);
    let pending=primary.querySelector('.member-verify').listeners.click();
    assert.deepEqual(sent,{name:'Alice',membershipId:'BKM-00001'});
    resolveRequest({verified:true,discountPercent:25}); await pending;
    assert.equal(context.membershipsReady(),true);
    assert.equal(elements.get('total-amount').textContent,'7.50');
    assert.match(primary.querySelector('.member-status').textContent,/25%/);
    name.value='Wrong name'; name.listeners.input();
    assert.equal(context.membershipsReady(),false);
    assert.equal(elements.get('total-amount').textContent,'10.00');
    pending=primary.querySelector('.member-verify').listeners.click();
    name.value='Alice'; name.listeners.input();
    resolveRequest({verified:true,discountPercent:25}); await pending;
    assert.equal(context.selectedMembershipId(primary),null,'Old response must not verify changed name');
    pending=primary.querySelector('.member-verify').listeners.click();
    rejectRequest(Error('Membership could not be verified.')); await pending;
    assert.equal(context.membershipsReady(),false);
    assert.match(primary.querySelector('.member-status').textContent,/could not/);
    pending=primary.querySelector('.member-verify').listeners.click();
    resolveRequest({verified:true,discountPercent:25}); await pending;
    const partner=node(), partnerName=node('Bob');hosts.push(partner);
    context.mountMembershipCheck(partner,partnerName);
    const dob=node('1990-01-01');cards.push({membershipHost:partner,querySelector:()=>dob});
    selectYes(partner,'Bob');pending=partner.querySelector('.member-verify').listeners.click();
    resolveRequest({verified:true,discountPercent:25});await pending;
    assert.equal(elements.get('total-amount').textContent,'15.00');
    primary.membershipState.choice.value='no';primary.membershipState.choice.listeners.change();
    assert.equal(elements.get('total-amount').textContent,'17.50');
    assert.equal(context.membershipsReady(),true);
    dob.value='2020-01-01';context.updatePriceSummary();
    assert.equal(elements.get('total-amount').textContent,'10.00','Child remains free');
    partner.membershipState.input.value='BKM-00002';partner.membershipState.input.listeners.input();
    assert.equal(context.membershipsReady(),false);
    console.log('PASS: verify button, couple discounts, children, errors, changed name/ID and stale response protection');
})().catch(error=>{console.error(error);process.exitCode=1;});
