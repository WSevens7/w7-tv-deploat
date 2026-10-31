import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

// Integration contract fixture: same API shape observed on the website, with more
// tracks than the live test title so that no language/resolution can be hardcoded.
let variants=[
  {id:1,width:640,height:360,language:'en',active:false},
  {id:2,width:1280,height:536,language:'en',active:true},
  {id:3,width:1920,height:804,language:'en',active:false},
  {id:4,width:1280,height:536,language:'es',active:false}
];
let subtitles=[{id:10,label:'Indonesian',language:'id',active:true},{id:11,label:'English',language:'en',active:false},{id:12,label:'Japanese',language:'ja',active:false}];
let auto=false,ad=false,present=true;
const player={
  getVariantTracks:()=>variants,getTextTracks:()=>subtitles,
  getConfiguration:()=>({abr:{enabled:auto}}),configure:c=>{auto=c.abr.enabled;},
  selectVariantTrack:t=>{for(const v of variants)v.active=v.id===t.id;},
  selectTextTrack:t=>{for(const s of subtitles)s.active=!!t&&s.id===t.id;}
};
const container={ui:{getControls:()=>({getPlayer:()=>player})},querySelector:()=>ad?{getBoundingClientRect:()=>({height:20})}:null};
const video={duration:600,currentTime:40,paused:true,playbackRate:1,readyState:4,buffered:{length:1,end:()=>70},
  closest:s=>s==='.shaka-video-container'?container:null,pause(){this.paused=true;},play(){this.paused=false;return Promise.resolve();}};
video.ui=container.ui;
const document={title:'Test title / IDLIX',documentElement:{classList:{remove(){}}},querySelector:s=>!present?null:s.includes('video.shaka-video')?video:s.includes('Tutup pemutar')?{click(){present=false;}}:null};
const stored=new Map();const sessionStorage={getItem:k=>stored.get(k)||null,setItem:(k,v)=>stored.set(k,v),removeItem:k=>stored.delete(k)};
const context=vm.createContext({window:{},document,location:{hostname:'z2.idlixku.com'},URL,sessionStorage,getComputedStyle:()=>({display:'block'})});
vm.runInContext(fs.readFileSync(new URL('../app/src/main/assets/tv-player.js',import.meta.url),'utf8'),context);
const api=context.window.__w7TV;
const state=()=>JSON.parse(JSON.stringify(api.poll(false,false)));
let checks=0;function check(name,fn){fn();checks++;console.log('PASS '+name);}
check('all subtitle languages come from source',()=>assert.deepEqual(state().subtitles.map(t=>t.label),['Nonaktif','Indonesian','English','Japanese']));
check('all source resolutions plus Auto, preserving cropped-height nominal quality',()=>assert.deepEqual(state().qualities.map(t=>t.label),['Otomatis','1080p','720p','360p']));
check('same resolution in another audio language does not duplicate quality',()=>assert.equal(state().qualities.filter(t=>t.label==='720p').length,1));
check('shared resolution label cannot hide a distinct real 360p stream',()=>{const saved=variants;try{variants=[{id:101,width:1280,height:720,label:'720p',active:true},{id:102,width:640,height:360,label:'720p'}];assert.deepEqual(state().qualities.map(t=>t.label),['Otomatis','720p','360p']);}finally{variants=saved;}});
check('unprobed streams with equal bitrate retain distinct quality choices',()=>{const saved=variants;try{variants=[{id:101,videoId:201,bandwidth:3500000,active:true},{id:102,videoId:202,bandwidth:3500000}];assert.deepEqual(state().qualities.map(t=>t.id),['auto','101','102']);}finally{variants=saved;}});
check('unprobed audio-language duplicates of the same video stream merge safely',()=>{const saved=variants;try{variants=[{id:101,videoId:201,language:'en',bandwidth:3500000},{id:102,videoId:201,language:'es',bandwidth:3500000,active:true}];assert.deepEqual(state().qualities.map(t=>t.id),['auto','102']);}finally{variants=saved;}});
check('later source tracks and dimensions appear on subsequent polls without guessing resolutions',()=>{const saved=variants;try{variants=[{id:106,videoId:105,bandwidth:3500000,active:true}];assert.equal(state().qualities[1].label,'3.5 Mbps');variants[0].width=1280;variants[0].height=720;variants.push({id:103,videoId:102,width:640,height:360});assert.deepEqual(state().qualities.map(t=>t.label),['Otomatis','720p','360p']);}finally{variants=saved;}});
check('subtitle selection uses actual source track ID',()=>{assert.equal(api.command('subtitle','12').ok,true);assert.equal(state().subtitle,'Japanese');});
check('modern Shaka subtitle Off uses supported null selection',()=>{api.command('subtitle','off');assert.equal(state().subtitle,'Nonaktif');});
check('Auto quality enables ABR, manual source quality disables it',()=>{api.command('quality','auto');assert.equal(auto,true);api.command('quality','3');assert.equal(auto,false);assert.equal(state().quality,'1080p');});
check('unknown quality ID cannot select a different source',()=>assert.equal(api.command('quality','not-a-track').ok,false));
check('a single-quality source remains single-quality',()=>{variants=[variants[1]];assert.deepEqual(state().qualities.map(t=>t.label),['Otomatis','720p']);});
check('source audio label is not mistaken for video quality',()=>{variants[0].label='audio';assert.equal(state().qualities[1].label,'720p');});
check('a real 540p source is not mislabeled as 480p',()=>{variants[0].width=960;variants[0].height=540;assert.equal(state().qualities[1].label,'540p');variants[0].width=1280;variants[0].height=536;});
check('seek stays within media boundaries',()=>{api.command('seek',-100);assert.equal(video.currentTime,0);api.command('position',9999);assert.equal(video.currentTime,599.9);});
check('website ad mode is left to the website',()=>{ad=true;assert.equal(state().available,false);assert.equal(api.command('seek',10).ok,false);ad=false;});
check('closed player is not treated as an active video',()=>{present=false;assert.equal(state().available,false);});
present=true;
Object.assign(context.location,{pathname:'/series/test/season/1/episode/5',origin:'https://z2.idlixku.com',href:'https://z2.idlixku.com/series/test/season/1/episode/5'});
let navigations=[];
const path=n=>'/series/test/season/1/episode/'+n;
function link(href,number,image=true){return {getAttribute:k=>k==='href'?href:null,querySelector:s=>s==='img'&&image?{alt:'Episode '+number,getAttribute:()=>`https://image.tmdb.org/t/p/w500/episode${number}.jpg`}:null,click:()=>navigations.push(href)};}
let links=[link(path(1),1,false),...[7,6,5,4,3,2,1].map(n=>link(path(n),n)),link(path(4),4,false),link('/series/test/season/2/episode/1',1),link('https://ads.example'+path(8),8),link('/series/other/season/1/episode/1',1)];
document.querySelectorAll=()=>links;
check('all seven actual episode links are sorted without breadcrumbs/previous-next duplicates',()=>assert.deepEqual(state().episodes.map(e=>e.number),[1,2,3,4,5,6,7]));
check('episode 5 is the sole current item even when source order starts at 7',()=>assert.deepEqual(state().episodes.filter(e=>e.current).map(e=>e.number),[5]));
check('only current series and season are exposed, excluding external links',()=>assert.equal(state().episodes.every(e=>e.season===1&&e.id.startsWith('/series/test/season/1/')),true));
check('thumbnail metadata prefers the real episode card over bare navigation links',()=>assert.equal(state().episodes[0].thumbnail,'https://image.tmdb.org/t/p/w300/episode1.jpg'));
check('choosing current episode does not restart it or request another preroll',()=>{api.command('episode',path(5));assert.equal(navigations.length,0);});
check('unknown episode is not invented or navigated to',()=>{assert.equal(api.command('episode',path(99)).ok,false);assert.equal(navigations.length,0);});
check('choosing episode 6 uses the source link and records its normal Play request',()=>{api.command('episode',path(6));assert.deepEqual(navigations,[path(6)]);assert.equal(JSON.parse(stored.get('w7-episode-request')).path,path(6));assert.equal(video.paused,true);});
check('movie pages do not expose unrelated episode rows',()=>{context.location.pathname='/movie/test';assert.deepEqual(state().episodes,[]);});
let playRequests=0;const originalQuery=document.querySelector;
const normalPlay={classList:{contains:name=>['absolute','inset-0'].includes(name)},click:()=>playRequests++};
document.querySelector=s=>s==='main'?{querySelectorAll:()=>[normalPlay]}:s==='.hero-player-playing video'?null:originalQuery(s);
context.location.pathname=path(6);context.location.href=context.location.origin+path(6);
check('episode switch presses the normal website Play button only once',()=>{api.poll(false,false);api.poll(false,false);assert.equal(playRequests,1);assert.equal(stored.has('w7-episode-request'),false);});
check('expired episode request cannot restart playback later',()=>{stored.set('w7-episode-request',JSON.stringify({path:path(6),time:Date.now()-121000}));api.poll(false,false);assert.equal(playRequests,1);assert.equal(stored.has('w7-episode-request'),false);});
check('already-started episode clears pending autoplay so closing it cannot reopen it',()=>{stored.set('w7-episode-request',JSON.stringify({path:path(6),time:Date.now()}));const query=document.querySelector;document.querySelector=s=>s==='video'?video:query(s);api.poll(false,false);assert.equal(stored.has('w7-episode-request'),false);assert.equal(playRequests,1);});
function movieHarness(fail=false){
 const requests=[],destinations=[],store=new Map();
 const loc={hostname:'z2.idlixku.com',origin:'https://z2.idlixku.com',pathname:'/movie/current',href:'https://z2.idlixku.com/movie/current',assign:p=>destinations.push(p)};
 const genre={textContent:'Comedy',getAttribute:()=>'/genre/comedy'};
 const v={duration:500,currentTime:20,paused:false,playbackRate:1,readyState:4,videoWidth:1280,videoHeight:720,buffered:{length:0},pause(){this.paused=true;}};
 const p={getTextTracks:()=>[],getVariantTracks:()=>[{id:1,width:1280,height:720,active:true}],getConfiguration:()=>({abr:{enabled:true}})};
 const c={querySelector:()=>null};v.closest=()=>c;v.ui={getControls:()=>({getPlayer:()=>p})};
 const doc={title:'Current movie / IDLIX',documentElement:{classList:{remove(){}}},querySelector:s=>s.includes('video.shaka-video')?v:s.includes('Tutup pemutar')?{click(){}}:null,querySelectorAll:s=>s.startsWith('main a[href^="/genre/')?[genre]:[]};
 const rows=[{contentType:'tv_series',slug:'series',title:'Do not include'},{contentType:'movie',slug:'current',title:'Current movie'},...Array.from({length:12},(_,i)=>({contentType:'movie',slug:'m'+(i+1),title:'Movie '+(i+1),backdropPath:'/still'+i+'.jpg',releaseDate:'2025-01-01'}))];rows.splice(4,0,rows[2]);
 const ctx=vm.createContext({window:{},document:doc,location:loc,URL,URLSearchParams,AbortController,setTimeout,clearTimeout,sessionStorage:{getItem:k=>store.get(k)||null,setItem:(k,v)=>store.set(k,v),removeItem:k=>store.delete(k)},fetch:async url=>{requests.push(url);if(fail)throw Error('unavailable');return {ok:true,json:async()=>({data:rows,pagination:{totalPages:2}})};}});
 vm.runInContext(fs.readFileSync(new URL('../app/src/main/assets/tv-player.js',import.meta.url),'utf8'),ctx);
 const api=ctx.window.__w7TV;return {api,v,doc,ctx,requests,destinations,store,read:()=>JSON.parse(JSON.stringify(api.poll(false,false)))};
}
const movie=movieHarness();movie.read();await new Promise(r=>setImmediate(r));
check('movie recommendations use the actual source genre in the public catalog request',()=>{assert.equal(new URL(movie.requests[0],'https://z2.idlixku.com').searchParams.get('genre'),'comedy');assert.equal(movie.read().recommendationGenre,'Comedy');});
check('movie recommendations are capped at eight unique movies, excluding current title and series',()=>{const list=movie.read().recommendations;assert.equal(list.length,8);assert.equal(new Set(list.map(x=>x.id)).size,8);assert.equal(list.every(x=>x.kind==='movie'&&x.id!=='/movie/current'&&x.id!=='/movie/series'),true);});
check('recommendation cache avoids repeated network requests while controls poll',()=>{movie.read();movie.read();assert.equal(movie.requests.length,1);});
check('movie recommendation uses source backdrop and title metadata',()=>{const m=movie.read().recommendations[0];assert.equal(m.title,'Movie 1');assert.equal(m.thumbnail,'https://image.tmdb.org/t/p/w300/still0.jpg');});
check('an unlisted recommendation cannot navigate to an invented movie',()=>{assert.equal(movie.api.command('movie','/movie/not-listed').ok,false);assert.equal(movie.destinations.length,0);});
check('selecting a recommendation follows normal movie page and Play request',()=>{assert.equal(movie.api.command('movie','/movie/m1').ok,true);assert.deepEqual(movie.destinations,['/movie/m1']);assert.equal(JSON.parse(movie.store.get('w7-episode-request')).path,'/movie/m1');assert.equal(movie.v.paused,true);});
const unavailable=movieHarness(true);unavailable.read();await new Promise(r=>setImmediate(r));
check('failed genre source does not fall back to unrelated or fabricated titles',()=>{assert.equal(unavailable.read().recommendationState,'error');assert.deepEqual(unavailable.read().recommendations,[]);});
check('protected video frames are not sampled for glass',()=>{const h=movieHarness();h.v.mediaKeys={};let created=0;h.doc.createElement=()=>{created++;};assert.equal(h.api.captureFrame(),null);assert.equal(created,0);});
check('cross-origin frame restriction falls back and is not retried for the same video',()=>{const h=movieHarness();let created=0;h.doc.createElement=()=>{created++;return {getContext:()=>({fillRect(){},drawImage(){throw Error('SecurityError');}})};};assert.equal(h.api.captureFrame(),null);assert.equal(h.api.captureFrame(),null);assert.equal(created,1);});
console.log(`${checks} checks passed`);
