(function () {
  if (window.__w7TV) return;
  const api = {lastError: ''};
  let mounted = null;
  const recommendationLimit=8, recommendationCache=new Map(), blockedFrames=new WeakSet();
  function recommendationData(){
    const path=location.pathname||'';
    if(!/^\/movie\/[^/]+\/?$/.test(path))return {recommendations:[],recommendationState:'none',recommendationGenre:'',recommendationLimit};
    let record=recommendationCache.get(path);
    if(!record){
      let genre=null;
      for(const a of document.querySelectorAll('main a[href^="/genre/"]')){
        let u;try{u=new URL(a.getAttribute('href'),location.href);}catch(_){continue;}
        const match=u.pathname.match(/^\/genre\/([^/]+)\/?$/),label=a.textContent?.trim();
        if(u.origin===location.origin&&match&&label){genre={slug:decodeURIComponent(match[1]),label:label.slice(0,70)};break;}
      }
      if(!genre)return {recommendations:[],recommendationState:'missing',recommendationGenre:'',recommendationLimit};
      record={items:[],status:'loading',genre:genre.label};recommendationCache.set(path,record);
      if(recommendationCache.size>5)recommendationCache.delete(recommendationCache.keys().next().value);
      loadRecommendations(path,genre,record);
    }
    return {recommendations:record.items,recommendationState:record.status,recommendationGenre:record.genre,recommendationLimit};
  }
  async function loadRecommendations(path,genre,record){
    const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),12000),seen=new Set([path.replace(/\/$/,'')]);
    try{
      // This is the same public, genre-filtered catalog endpoint used by the site's genre page.
      // At most two small catalog pages; only eight movie cards/images are exposed to Android.
      for(let page=1;page<=2&&record.items.length<recommendationLimit;page++){
        const query=new URLSearchParams({page:String(page),limit:'36',sort:'latest',genre:genre.slug});
        const response=await fetch('/api/browse?'+query,{credentials:'same-origin',signal:controller.signal});
        if(!response.ok)throw Error('catalog unavailable');const data=await response.json();
        if(!Array.isArray(data.data))throw Error('catalog format changed');
        for(const item of data.data){
          if(item.contentType!=='movie'||typeof item.slug!=='string'||!item.slug||/[\/?#]/.test(item.slug)||!item.title)continue;
          const id='/movie/'+encodeURIComponent(item.slug);if(seen.has(id))continue;seen.add(id);
          const image=item.backdropPath||item.posterPath;
          const thumbnail=typeof image==='string'&&/^\/[A-Za-z0-9_./-]+$/.test(image)?'https://image.tmdb.org/t/p/w300'+image:'';
          record.items.push({id,kind:'movie',title:String(item.title).slice(0,160),thumbnail,genre:genre.label,year:String(item.releaseDate||'').slice(0,4),current:false});
          if(record.items.length===recommendationLimit)break;
        }
        if(!data.pagination||page>=Number(data.pagination.totalPages||1))break;
      }
      record.status=record.items.length?'ready':'empty';
    }catch(_){record.status=record.items.length?'ready':'error';}finally{clearTimeout(timer);}
  }
  const episodePattern = /^\/series\/([^/]+)\/season\/(\d+)\/episode\/(\d+)\/?$/;
  function episodeData() {
    const current = (location.pathname || '').match(episodePattern);
    if (!current) return {episodes:[], episodeId:'', season:0};
    const byPath = new Map();
    for (const a of document.querySelectorAll('a[href]')) {
      let u;try {u=new URL(a.getAttribute('href'),location.href);}catch(_){continue;}
      const m=u.pathname.match(episodePattern);
      if(u.origin!==location.origin||!m||m[1]!==current[1]||m[2]!==current[2])continue;
      const image=a.querySelector('img'), heading=a.querySelector('h3,h4');
      const previous=byPath.get(u.pathname);
      if(previous && (!image || previous.thumbnail))continue;
      const label=(heading?.textContent || image?.alt || 'Episode '+Number(m[3])).trim().slice(0,100);
      let thumbnail=image?.getAttribute('src') || '';
      if(thumbnail.includes('image.tmdb.org/t/p/'))thumbnail=thumbnail.replace(/\/t\/p\/[^/]+\//,'/t/p/w300/');
      byPath.set(u.pathname,{id:u.pathname,number:Number(m[3]),season:Number(m[2]),title:label,thumbnail,current:Number(m[3])===Number(current[3])});
    }
    return {episodes:[...byPath.values()].sort((a,b)=>a.number-b.number),episodeId:location.pathname,season:Number(current[2])};
  }
  // A native episode selection follows the site's normal page and Play button, including its ads.
  function resumeEpisodeRequest(){
    let pending;try{pending=JSON.parse(sessionStorage.getItem('w7-episode-request')||'null');}catch(_){return;}
    if(!pending)return;
    if(Date.now()-pending.time>120000){sessionStorage.removeItem('w7-episode-request');return;}
    if(pending.path!==location.pathname)return;
    if(document.querySelector('video')){sessionStorage.removeItem('w7-episode-request');return;}
    const main=document.querySelector('main');
    const play=main&&[...main.querySelectorAll('button')].find(b=>b.classList.contains('absolute')&&b.classList.contains('inset-0'));
    if(play){sessionStorage.removeItem('w7-episode-request');play.click();}
  }
  const allowed = () => location.hostname === 'idlixku.com' || location.hostname.endsWith('.idlixku.com');
  function find() {
    if (!allowed()) return {};
    const close = document.querySelector('button[aria-label="Tutup pemutar"]');
    const v = document.querySelector('.hero-player-playing video.shaka-video');
    const c = v && v.closest('.shaka-video-container');
    let p;
    try { p = (v?.ui || c?.ui)?.getControls?.().getPlayer?.(); } catch (_) {}
    let available = !!(close && v && p && Number.isFinite(v.duration) && v.duration > 0 && p.getVariantTracks().length);
    const ad = c?.querySelector('.shaka-ad-controls');
    if (ad && getComputedStyle(ad).display !== 'none' && ad.getBoundingClientRect().height > 0) available = false;
    return {v, c, p, close, available};
  }
  function release() {
    if(mounted)window.__w7WebMenus?.close({c:mounted.querySelector('.shaka-video-container')});
    if (mounted) mounted.classList.remove('w7-tv-player');
    document.documentElement.classList.remove('w7-tv-mode');
    mounted = null;
  }
  function mount(f, shown) {
    if (!document.getElementById('w7-tv-style')) {
      const s = document.createElement('style'); s.id = 'w7-tv-style';
      s.textContent = `
        html.w7-tv-mode,html.w7-tv-mode body{overflow:hidden!important}
        .w7-tv-player{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;max-height:none!important;margin:0!important;padding:0!important;transform:none!important;z-index:2147483000!important;background:#000!important}
        .w7-tv-player .hero-player-frame{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;max-width:none!important;max-height:none!important;aspect-ratio:auto!important;border-radius:0!important}
        .w7-tv-player .shaka-video-container{width:100%!important;height:100%!important}
        .w7-tv-player video{width:100%!important;height:100%!important;object-fit:contain!important;background:#000!important}
        .w7-tv-player .shaka-controls-container,.w7-tv-player button[aria-label="Tutup pemutar"]{visibility:hidden!important;pointer-events:none!important}
        html.w7-tv-mode .player-caption-style-panel{visibility:hidden!important;pointer-events:none!important}
        .w7-tv-player .shaka-text-container{padding-bottom:var(--w7-caption-bottom,32px)!important;box-sizing:border-box!important}
      `;
      document.head.appendChild(s);
    }
    const host = f.v.closest('.hero-player-playing');
    if (!host) return false;
    if (mounted && mounted !== host) release();
    mounted = host;host.classList.add('w7-tv-player');document.documentElement.classList.add('w7-tv-mode');
    host.style.setProperty('--w7-caption-bottom', (typeof shown==='number'?Math.max(32,Math.min(320,shown)):shown?190:32)+'px');
    return true;
  }
  function nominal(t) {
    const h=Number(t.height)||0,w=Number(t.width)||0;
    const n=[2160,1440,1080,720,576,540,480,360,240,144].includes(h)?h:w>=3800?2160:w>=2500?1440:w>=1900?1080:w>=1200?720:w>=960?540:w>=850?480:w>=640?360:w>=426?240:h;
    if(n)return n+'p';
    const explicit=[t.videoLabel,t.label].find(s=>typeof s==='string'&&/\b(?:\d{3,4}p|[248]k)\b/i.test(s));
    return explicit|| (t.bandwidth?(t.bandwidth/1000000).toFixed(1)+' Mbps':'Bawaan');
  }
  function qualityKey(t){
    if(Number(t.width)>0||Number(t.height)>0)return 'resolution:'+nominal(t);
    // A bitrate or generic label cannot prove that two unprobed video streams are identical.
    return 'video:'+String(t.videoId??t.originalVideoId??t.id);
  }
  function state(f) {
    const {v,p} = f, text = p.getTextTracks(), variants = p.getVariantTracks(), current = variants.find(t => t.active);
    let visible = text.some(t=>t.active);
    if (typeof p.isTextTrackVisible === 'function') visible = p.isTextTrackVisible();
    const auto = !!p.getConfiguration().abr.enabled;
    const seen = new Set(), quality = [{id:'auto', label:'Otomatis', selected:auto}];
    for (const t of [...variants].sort((a,b)=>(b.width||0)-(a.width||0))) {
      const label = nominal(t),key=qualityKey(t);if (seen.has(key)) continue;seen.add(key);
      const matching = variants.find(q=>qualityKey(q)===key && q.language===current?.language) || t;
      quality.push({id:String(matching.id),label,selected:!auto && !!current && qualityKey(current)===key});
    }
    const subtitles = [{id:'off', label:'Nonaktif', selected:!visible}, ...text.map(t=>({id:String(t.id),label:t.label||t.language||'Subtitle',selected:visible&&t.active}))];
    const webMenus=window.__w7WebMenus?.read(f,{qualities:quality,subtitles})||{};
    const sourceQualities=webMenus.quality?.items||quality,sourceSubtitles=webMenus.captionLanguage?.items||subtitles;
    let buffered=0;try{if(v.buffered.length)buffered=v.buffered.end(v.buffered.length-1);}catch(_){}
    const error=api.lastError;api.lastError='';
    return {available:true, title:document.title.replace(/\s*\/\s*IDLIX.*$/i,'').slice(0,150),time:v.currentTime,duration:v.duration,paused:v.paused,buffered,waiting:!v.paused&&v.readyState<3,
      subtitle:sourceSubtitles.find(t=>t.selected)?.label||'Nonaktif',quality:sourceQualities.find(t=>t.selected)?.label||(auto?'Otomatis':current?nominal(current):'—'),rate:v.playbackRate,subtitles:sourceSubtitles,qualities:sourceQualities,webMenus,contentId:location.pathname,contentType:(location.pathname||'').startsWith('/movie/')?'movie':'series',...episodeData(),...recommendationData(),error};
  }
  api.poll = function (enabled, shown) {
    try {
      if(allowed())resumeEpisodeRequest();
      const f=find();if(!f.available){release();return {available:false};}
      if(enabled) mount(f,shown);else release();return state(f);
    } catch (_) {release();return {available:false};}
  };
  api.command = function (cmd,arg) {
    try {
      const f=find();if(!f.available)return {ok:false,error:'Player belum siap.'};
      const {v,p}=f;
      if(cmd==='prepareMenu'){window.__w7WebMenus?.prepare(f,String(arg));return {ok:true};}
      if(cmd==='webAction'){
        if(!arg||typeof arg.id!=='string'||!window.__w7WebMenus)return {ok:false,error:'Pilihan web belum tersedia.'};
        state(f);return window.__w7WebMenus.run(arg.id,arg.value);
      }
      if(cmd==='toggle') {if(v.paused)v.play().catch(()=>{api.lastError='Tekan Putar di web terlebih dahulu.';});else v.pause();}
      else if(cmd==='seek') v.currentTime=Math.max(0,Math.min(v.duration-.1,v.currentTime+Number(arg)));
      else if(cmd==='position') v.currentTime=Math.max(0,Math.min(v.duration-.1,Number(arg)));
      else if(cmd==='subtitle') {
        if(typeof arg==='string'&&arg.startsWith('web:')){state(f);return window.__w7WebMenus.run(arg);}
        if(arg==='off') {if(typeof p.setTextTrackVisibility==='function')p.setTextTrackVisibility(false);else p.selectTextTrack(null);}
        else {const t=p.getTextTracks().find(t=>String(t.id)===String(arg));if(!t)return {ok:false,error:'Subtitle tidak tersedia.'};p.selectTextTrack(t);if(typeof p.setTextTrackVisibility==='function')p.setTextTrackVisibility(true);}
      }
      else if(cmd==='quality') {
        if(typeof arg==='string'&&arg.startsWith('web:')){state(f);return window.__w7WebMenus.run(arg);}
        if(arg==='auto') p.configure({abr:{enabled:true}});
        else {const t=p.getVariantTracks().find(t=>String(t.id)===String(arg));if(!t)return {ok:false,error:'Kualitas tidak tersedia.'};p.configure({abr:{enabled:false}});p.selectVariantTrack(t,true,8);}
      }
      else if(cmd==='rate') {const n=Number(arg);if([.5,.75,1,1.25,1.5,2].includes(n))v.playbackRate=n;}
      else if(cmd==='episode') {
        const item=episodeData().episodes.find(e=>e.id===arg);
        if(!item)return {ok:false,error:'Episode tidak tersedia pada daftar sumber.'};
        if(item.current)return {ok:true};
        const link=[...document.querySelectorAll('a[href]')].find(a=>{try{return new URL(a.getAttribute('href'),location.href).pathname===item.id;}catch(_){return false;}});
        if(!link)return {ok:false,error:'Tautan episode tidak tersedia.'};
        try{sessionStorage.setItem('w7-episode-request',JSON.stringify({path:item.id,time:Date.now()}));}catch(_){}
        v.pause();release();link.click();
      }
      else if(cmd==='movie') {
        const record=recommendationCache.get(location.pathname),item=record?.items.find(e=>e.id===arg);
        if(!item||!/^\/movie\/[^/]+$/.test(item.id))return {ok:false,error:'Film tidak ada pada rekomendasi sumber.'};
        try{sessionStorage.setItem('w7-episode-request',JSON.stringify({path:item.id,time:Date.now()}));}catch(_){}
        v.pause();release();location.assign(item.id);
      }
      else if(cmd==='close') {release();f.close.click();}
      else if(cmd==='release') release();
      return {ok:true};
    } catch (_) {return {ok:false,error:'Kontrol situs berubah. Gunakan Kontrol web.'};}
  };
  api.captureFrame=function(){
    const f=find(),v=f.v;if(!f.available||!v||v.readyState<2||v.mediaKeys||blockedFrames.has(v))return null;
    try{
      const canvas=document.createElement('canvas');canvas.width=192;canvas.height=108;
      const c=canvas.getContext('2d',{alpha:false}),scale=Math.min(192/v.videoWidth,108/v.videoHeight);
      c.fillStyle='#000';c.fillRect(0,0,192,108);c.drawImage(v,(192-v.videoWidth*scale)/2,(108-v.videoHeight*scale)/2,v.videoWidth*scale,v.videoHeight*scale);
      return canvas.toDataURL('image/jpeg',.5);
    }catch(_){blockedFrames.add(v);return null;}
  };
  window.__w7TV=api;
})();
