const fs=require('fs'),vm=require('vm'),assert=require('assert/strict');
const attack=`O'Brien " autofocus onfocus="alert(1)"><img src=x onerror=alert(1)>`;
class Element {
 constructor(tag){this.tag=tag;this.children=[];this.dataset={};this.style={};this.events={};this.textContent='';}
 append(...nodes){this.children.push(...nodes);} appendChild(node){this.append(node);}
 replaceChildren(...nodes){this.children=nodes;}
 addEventListener(name,fn){this.events[name]=fn;}
 set innerHTML(value){assert.ok(!value.includes(attack),'Untrusted input reached HTML parser');this.children=[];}
}
const elements=new Map();const get=id=>{if(!elements.has(id))elements.set(id,new Element('div'));return elements.get(id);};
const context={URL,console,document:{getElementById:get,createElement:tag=>new Element(tag),createTextNode:text=>({textContent:text}),addEventListener(){}},window:{location:{origin:'http://localhost:8084'},addEventListener(){}},Sortable:{create:()=>({destroy(){}})},localStorage:{getItem:()=>null}};
context.location={pathname:'/index.html',search:''};
vm.createContext(context);
for(const file of ['common','admin_dashboard','admin_sponsors','home','footer'])vm.runInContext(fs.readFileSync(`frontend/js/${file}.js`,'utf8'),context);
function all(node){return [node,...node.children.flatMap(n=>n.children?all(n):[n])];}
(async()=>{
 context.apiFetchWithAuth=async()=>[{id:1,name:attack,email:attack,message:attack,replyText:attack,read:false}];
 await context.loadMessages();
 const reply=all(get('messages-list')).find(n=>n.className?.includes('reply-btn'));
 let received;context.openReplyModal=(...args)=>received=args;reply.events.click();assert.equal(received[1],attack);assert.equal(received[3],attack);
 const sponsor={id:1,name:attack,address:attack,description:attack,websiteUrl:'javascript:alert(1)',logoPath:'javascript:alert(1)'};
 context.renderSponsorList([sponsor]);let deleted;context.deleteSponsor=(...args)=>deleted=args;
 all(get('sponsor-list')).find(n=>n.title==='Delete').events.click();assert.equal(deleted[1],attack);
 context.apiFetch=async()=>[sponsor];await context.loadFooterSponsors();
 assert.equal(all(get('footer-sponsor-cards')).filter(n=>n.tag==='a'||n.tag==='img').length,0);
 context.apiFetch=async url=>url==='/api/config/current'?{eventEditionId:1,eventYear:2026}:[{title:attack,description:attack,iconUrl:'javascript:alert(1)'}];await context.loadEventCards();
 assert.equal(all(get('event-cards')).find(n=>n.tag==='h3').textContent,attack);
 assert.equal(context.safeWebUrl('JaVaScRiPt:alert(1)'),'');assert.equal(context.safeWebUrl('data:text/html,test'),'');
 assert.equal(context.safeWebUrl('/uploads/test.png'),'http://localhost:8084/uploads/test.png');
 assert.equal(context.safeWebUrl('https://example.com'),'https://example.com/');
 assert.ok(!context.escapeHtml(attack).includes('"'));
 console.log('PASS: contact text/reply values, apostrophe sponsor actions, event/footer text, unsafe URL rejection and attribute escaping');
})().catch(e=>{console.error(e);process.exitCode=1;});
