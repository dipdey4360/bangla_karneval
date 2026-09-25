const {chromium}=require('playwright');
const http=require('http'),fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
const root=path.resolve('frontend');
const mime={'.html':'text/html','.css':'text/css','.js':'text/javascript','.svg':'image/svg+xml','.png':'image/png','.jpg':'image/jpeg'};
const server=http.createServer((req,res)=>{
 const target=path.resolve(root,'.'+decodeURIComponent(req.url.split('?')[0]==='/'?'/index.html':req.url.split('?')[0]));
 if(!target.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(target,(err,data)=>{if(err){res.writeHead(404);return res.end();}res.setHeader('Content-Type',mime[path.extname(target)]||'application/octet-stream');res.end(data);});
});
const current={themeKey:'eid',accentColor:'#146b52',eventEditionId:3,programmeCode:'bangla-karneval',title:'Bangla Karneval 2026',eventYear:2026,eventDate:'2026-05-15',eventLocation:'Wesseling Community Center, Germany',posterPath:'/assets/images/home_background.jpg',tagline:'Experience a day of Bengali music, food and festive spirit.',registrationEnabled:true,performerEnabled:true,pricePerPerson:20,version:1};
const events=[{...current,eventEditionId:1,programmeCode:'eid',title:'Eid 2026',eventDate:'2026-03-20',posterPath:'/assets/images/programmes/Eid.svg',tagline:'Celebrate Eid with warmth, togetherness and shared joy.'},{...current,eventEditionId:2,programmeCode:'bangla-noboborsho',title:'Noboborsho 2026',eventDate:'2026-04-14',posterPath:'/assets/images/programmes/Bangla_Noboborsho.svg'},current,{...current,eventEditionId:4,programmeCode:'bbq',title:'BBQ 2026',eventDate:null,posterPath:'/assets/images/programmes/bbq.png',tagline:'Enjoy good food, fresh air and great company.'},{...current,eventEditionId:5,programmeCode:'puja',title:'Puja 2026',eventDate:'2026-10-15',posterPath:'/assets/images/programmes/puja.svg'}];
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const browser=await chromium.launch({headless:true,channel:process.env.BK_BROWSER_CHANNEL || undefined});
 try {
 const page=await browser.newPage({viewport:{width:1280,height:1000}});let errors=[];page.on('pageerror',error=>errors.push(error.message));
 await page.route('https://**/*',route=>route.abort());
 await page.route('**/api/**',route=>{
  const url=new URL(route.request().url()).pathname;let data=[];
  if(url==='/api/config/current'||url==='/api/admin/editions/3')data=current;
  else if(url==='/api/editions/home')data=events;
  else if(url==='/api/sponsors')data=[{name:'Community sponsor',description:'Supporting our cultural events.'},{name:'Second sponsor'}];
  else if(url==='/api/editions')data=events;
  else if(url==='/api/organisation')data={name:'Bangla Karneval e.V.',story:fs.readFileSync('database/migrations/20260924_organisation_story.sql','utf8').split('$story$')[1],contactEmail:'club@example.invalid',contactFacebook:'https://facebook.com/example'};
  else if(url==='/api/membership/settings')data={benefits:'Community\nEvent discounts',singleFee:25,coupleFee:30,paymentInstructions:'Contact the board.',memberDiscountPercent:25};
  else if(url.endsWith('/activities'))data=[{title:'Music and performance',description:'Enjoy performances from our community.'},{title:'Food and traditions',description:'Share familiar flavours and discover something new.'}];
  else if(url==='/api/register/verify-membership')data={verified:true,discountPercent:25};
  return route.fulfill({json:data});
 });
 const base='http://127.0.0.1:'+server.address().port;
 await page.goto(base+'/index.html');await page.locator('#selected-event-title').filter({hasText:'Bangla Karneval 2026'}).waitFor();
 assert.equal(await page.locator('#hero-register-btn').isVisible(),true);
 assert.match(await page.title(),/^Bangla Karneval e.V./);
 assert.equal(await page.locator('.club-eyebrow').textContent(),'A little Bangla. A shared home.');
 assert.equal(await page.locator('.event-slide').count(),5);
 await page.locator('#event-next').click();assert.equal(await page.locator('#hero-register-btn').isVisible(),false);
 await page.locator('#event-selector button').filter({hasText:'Eid 2026'}).click();assert.match(await page.locator('#selected-event-tagline').textContent(),/Celebrate Eid/);
 assert.equal(await page.locator('#event-previous').isDisabled(),true);
 await page.locator('#event-selector button').filter({hasText:'Bangla Karneval 2026'}).click();
 await page.locator('#header-container .site-header').waitFor();await page.waitForTimeout(550);
 assert.equal(await page.locator('.nav-register-btn').count(),0);
 assert.equal(await page.locator('a.header-logo').count(),0);
 assert.match(await page.locator('#hero-register-btn').getAttribute('href'),/edition=3$/);
 const output=process.env.BK_PREVIEW_DIR;
 if(output)await page.screenshot({path:path.join(output,'club-home-desktop.png'),fullPage:true});
 for(const width of [1280,768,390,320]){
  await page.setViewportSize({width,height:950});
  for(const file of ['index.html','about_us.html','gallery.html','contact.html','registration.html','performer_registration.html']){
   await page.goto(base+'/'+file);await page.locator('#header-container .site-header').waitFor();
   const sizes=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,viewport:innerWidth,bg:getComputedStyle(document.body).backgroundColor}));
   assert.ok(sizes.scroll<=sizes.viewport+1,`${file} overflow at ${width}: ${sizes.scroll}`);
   assert.equal(sizes.bg,'rgb(255, 250, 240)',file+' theme');
   assert.match(await page.locator('body').evaluate(el=>getComputedStyle(el,'::after').backgroundImage),/cologne-cathedral/);
   const layout=await page.locator('.container').first().boundingBox();
   assert.ok(layout.width<=1161 && layout.width<=width+1, file+' retains centered content limit');
   await page.locator('.footer-sponsors .sponsor-name').first().waitFor();
   const sponsorBox=await page.locator('.footer-sponsors > .container').boundingBox();
   assert.ok(sponsorBox.width<=581 && sponsorBox.width<=width+1,'Compact sponsor width');
   if(file==='performer_registration.html'){
    const boxes=await page.locator('.performance-type-option label').evaluateAll(els=>els.map(el=>el.getBoundingClientRect().height));
    assert.ok(Math.max(...boxes)-Math.min(...boxes)<1,'All performance choices have equal height');
   }
   if(file==='gallery.html')assert.equal(await page.locator('.highlights-section').count(),0);
   if(file==='registration.html'){
    await page.locator('#primary-membership .member-choice').waitFor();
    await page.locator('#primary-name').fill('Alice');await page.locator('#primary-membership .member-choice').selectOption('yes');
    await page.locator('#primary-membership .member-id').fill('BKM-00001');await page.locator('#primary-membership .member-verify').click();
    await page.locator('#primary-membership .member-status').filter({hasText:'Membership verified'}).waitFor();
    assert.equal(await page.locator('#total-amount').textContent(),'€15.00');
   }
   if(file==='about_us.html'){
    await page.locator('#about-text p').filter({hasText:'Our journey began in a small pub in Cologne'}).waitFor();
    assert.equal(await page.locator('#about-text > p').count(),5);
    assert.equal(await page.locator('#about-event-date, #about-event-location').count(),0);
    assert.equal(await page.locator('.about-grid > .about-text').count(),1);
    await page.locator('#membership-apply').click();assert.equal(await page.locator('#membership-dialog').isVisible(),true);await page.locator('#membership-close').click();
   }
   if(width===390 && output && ['index.html','registration.html','about_us.html'].includes(file))await page.screenshot({path:path.join(output,'club-'+file.replace('.html','')+'-mobile.png'),fullPage:true});
  }
 }
 await page.setViewportSize({width:390,height:850});await page.goto(base+'/index.html');await page.locator('#hamburger').click();assert.equal(await page.locator('#main-nav').isVisible(),true);await page.keyboard.press('Escape');assert.equal(await page.locator('#main-nav').isVisible(),false);
 await page.evaluate(()=>localStorage.setItem('adminToken','browser-test-token'));
 await page.goto(base+'/event_preview.html?edition=3');
 await page.locator('#selected-event-title').filter({hasText:'Bangla Karneval 2026'}).waitFor();
 assert.equal(await page.locator('#hero-register-btn').isVisible(),false);
 assert.match(await page.locator('#event-registration-status').textContent(),/preview/);
 assert.equal(await page.locator('#hero-register-btn').evaluate(el=>getComputedStyle(el).backgroundColor),'rgb(20, 107, 82)');
 await page.goto(base+'/registration.html?edition=999');
 await page.locator('#form-alert').filter({hasText:'no longer active'}).waitFor();
 assert.equal(await page.locator('#registration-form').isVisible(),false);
 assert.deepEqual(errors,[]);
 console.log('PASS: all six public pages at desktop/tablet/mobile widths, carousel, registration eligibility, membership dialog and mobile navigation');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>server.close());

