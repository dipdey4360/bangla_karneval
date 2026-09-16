const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

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
