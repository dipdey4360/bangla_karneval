const fs=require('fs'),vm=require('vm'),assert=require('assert/strict');
class Node {
 constructor(tag){this.tag=tag;this.children=[];this.dataset={};this.style={};this.events={};this.classList={add(){},remove(){},toggle(){}};}
 append(...nodes){this.children.push(...nodes);} appendChild(n){this.append(n);} replaceChildren(...nodes){this.children=nodes;}
 addEventListener(name,fn){this.events[name]=fn;} setAttribute(name,value){this[name]=value;}
 removeAttribute(name){delete this[name];} pause(){this.paused=true;} load(){}
 set innerHTML(value){this.children=[];this.html=value;}
 querySelectorAll(){return this.children;}
}
const nodes=new Map(); const get=id=>{if(!nodes.has(id))nodes.set(id,new Node('div'));return nodes.get(id);};
const requests=[];
const context={console,URL,window:{location:{origin:'http://localhost:8084'},addEventListener(){}},document:{body:new Node('body'),createElement:t=>new Node(t),getElementById:get,addEventListener(){},querySelectorAll:()=>get('year-tabs').children}};
vm.createContext(context);
for(const f of ['common','gallery'])vm.runInContext(fs.readFileSync(`frontend/js/${f}.js`,'utf8'),context);
context.apiFetch=async url=>{requests.push(url);if(url==='/api/config/current')return {eventYear:2026};if(url==='/api/gallery/years')return [2025,'2026'];return [{url:'/assets/test.mp4',mediaType:'VIDEO',caption:`O'Brien <img onerror=alert(1)>`}];};
(async()=>{
 await context.initGallery();
 assert.deepEqual(get('year-tabs').children.map(n=>n.textContent),['2026','2025']);
 assert.ok(requests.includes('/api/gallery/2026'));assert.ok(requests.includes('/api/gallery/2026?highlight=true'));
 const card=get('year-gallery-grid').children[0];assert.equal(card.children[0].tag,'video');card.events.click();
 assert.equal(get('lightbox-video').hidden,false);assert.equal(get('lightbox-image').hidden,true);
 assert.equal(get('lightbox-video').src,'http://localhost:8084/assets/test.mp4');
 context.closeLightbox();assert.equal(get('lightbox-video').src,undefined);assert.equal(get('lightbox-video').paused,true);
 context.openLightbox({url:'/assets/photo.png',mediaType:'IMAGE',caption:'Photo'});
 assert.equal(get('lightbox-video').hidden,true);assert.equal(get('lightbox-image').hidden,false);
 assert.ok(!/\.highlights-section\s*\{[^}]*display:\s*none/.test(fs.readFileSync('frontend/css/gallery.css','utf8')));
 console.log('PASS: current-year non-highlight gallery, year normalization, video/image rendering and playback cleanup');
})().catch(e=>{console.error(e);process.exitCode=1;});
