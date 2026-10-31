// Run with inspect-player.mjs in a debug WebView after loading web-menu-adapter.js.
// Uses a hidden, isolated DOM fixture. No real source preference or stream is changed.
(()=>{
  const adapter=window.__w7WebMenus;if(!adapter)throw Error('Load web-menu-adapter.js first');
  const fixture=document.createElement('div');fixture.className='hero-player-playing';fixture.style.display='none';
  const radio=(text,cls='')=>`<button class="${cls}" role="menuitemradio" aria-checked="false"><span>${text}</span></button>`;
  const submenu=(title,values)=>`<div class="shaka-sub-menu"><button class="shaka-back-to-overflow-button"><span>${title}</span></button>${values.map(v=>radio(v)).join('')}</div>`;
  fixture.innerHTML=`<div class="shaka-video-container"><video></video>
    <button class="shaka-resolution-button" aria-label="Resolusi"></button>
    <div class="shaka-resolutions">${[1080,720,480,360].map(n=>radio(n+'p','explicit-resolution')).join('')}${radio('Otomatis','shaka-enable-abr-button')}</div>
    <button class="shaka-caption-button" aria-label="Subtitle"></button>
    <div class="shaka-text-languages">${radio('Indonesian')}${radio('English')}${radio('Japanese')}${radio('Nonaktif','shaka-turn-captions-off-button')}<button class="caption-upload">Unggah subtitle</button></div>
    <button class="shaka-caption-size-button" aria-label="Ukuran subtitle"><span class="shaka-current-selection-span">100%</span></button>
    ${submenu('Ukuran subtitle',['50%','75%','100%','125%','150%','175%','200%'])}
    <button class="shaka-caption-position-button" aria-label="Posisi subtitle"><span class="shaka-current-selection-span">Bawaan</span></button>
    ${submenu('Posisi subtitle',['Bawaan','Kiri atas','Atas','Kanan atas','Kiri','Tengah','Kanan','Kiri bawah','Bawah','Kanan bawah'])}
    <button class="nb-caption-style-overflow-button" aria-label="Gaya subtitle"><span class="shaka-current-selection-span">Kustom</span></button>
    <div class="player-caption-style-panel">
      <button class="player-caption-style-panel__icon">close</button>
      <div class="player-caption-style-panel__row"><label>Warna</label><button class="player-caption-style-panel__swatch" aria-label="Hitam" aria-pressed="true"></button><button class="player-caption-style-panel__swatch" aria-label="Merah" aria-pressed="false"></button></div>
      <div class="player-caption-style-panel__row"><label>Opasitas</label><input type="range" id="caption-bg-opacity" min="0" max="1" step="0.05" value="0.55"><span class="player-caption-style-panel__value">55%</span></div>
      <div class="player-caption-style-panel__row"><label>Blur</label><input type="range" id="caption-bg-blur" min="0" max="12" step="1" value="0"><span class="player-caption-style-panel__value">0px</span></div>
      <div class="player-caption-style-panel__latency"><div class="player-caption-style-panel__latency-header">Jeda waktu</div><button class="player-caption-style-panel__stepper" aria-label="Lebih awal">-0.5s</button><div class="player-caption-style-panel__latency-value">0s</div><button class="player-caption-style-panel__stepper" aria-label="Lebih lambat">+0.5s</button></div>
      <button class="player-caption-style-panel__reset">Atur ulang</button>
    </div></div>`;
  document.body.appendChild(fixture);
  const f={v:fixture.querySelector('video'),c:fixture.querySelector('.shaka-video-container')};
  const fallback={qualities:[{id:'auto',label:'Otomatis',selected:true},...[1080,720,480,360].map(n=>({id:String(n),label:n+'p'}))],subtitles:[{id:'off',label:'Nonaktif'},{id:'id',label:'Indonesian',selected:true},{id:'en',label:'English'},{id:'ja',label:'Japanese'}]};
  const checks=[];const assert=(name,value)=>{if(!value)throw Error(name);checks.push(name);};
  const read=()=>adapter.read(f,fallback);let upload=0,clickedQuality='',inputEvents=0,changeEvents=0,delay=0,resets=0;
  for(const menu of fixture.querySelectorAll('.shaka-resolutions,.shaka-text-languages,.shaka-sub-menu')){
    for(const b of menu.querySelectorAll('button[role=menuitemradio]'))b.addEventListener('click',()=>{
      for(const q of menu.querySelectorAll('button[role=menuitemradio]'))q.setAttribute('aria-checked',String(q===b));
      if(menu.classList.contains('shaka-resolutions'))clickedQuality=b.textContent;
    });
  }
  fixture.querySelector('.caption-upload').onclick=()=>upload++;
  const opacity=fixture.querySelector('#caption-bg-opacity'),blur=fixture.querySelector('#caption-bg-blur');
  opacity.addEventListener('input',()=>{inputEvents++;opacity.nextElementSibling.textContent=Math.round(Number(opacity.value)*100)+'%';});
  opacity.addEventListener('change',()=>changeEvents++);
  blur.addEventListener('input',()=>blur.nextElementSibling.textContent=blur.value+'px');
  for(const b of fixture.querySelectorAll('.player-caption-style-panel__swatch'))b.onclick=()=>{for(const q of fixture.querySelectorAll('.player-caption-style-panel__swatch'))q.setAttribute('aria-pressed',String(q===b));};
  [...fixture.querySelectorAll('.player-caption-style-panel__stepper')].forEach((b,i)=>b.onclick=()=>{delay+=i?.5:-.5;fixture.querySelector('.player-caption-style-panel__latency-value').textContent=delay+'s';});
  fixture.querySelector('.player-caption-style-panel__reset').onclick=()=>resets++;
  fixture.querySelector('.player-caption-style-panel__icon').onclick=()=>fixture.querySelector('.player-caption-style-panel').remove();
  try{
    let menus=read();
    assert('four source resolutions plus Auto are mirrored without limits',menus.quality.items.map(t=>t.label).join(',')==='1080p,720p,480p,360p,Otomatis');
    const chosen=menus.quality.items.find(t=>t.label==='480p');adapter.run(chosen.id);menus=read();
    assert('quality selection invokes the actual source button',clickedQuality==='480p'&&menus.quality.items.find(t=>t.label==='480p').selected);
    const duplicate=document.createElement('button');duplicate.className='explicit-resolution';duplicate.textContent='360p';duplicate.onclick=()=>clickedQuality='duplicate360';fixture.querySelector('.shaka-resolutions').appendChild(duplicate);
    const twins=read().quality.items.filter(t=>t.label==='360p');adapter.run(twins[0].id);const firstWorks=clickedQuality==='360p';adapter.run(twins[1].id);
    assert('same-label source choices remain distinct and invoke their own buttons',twins.length===2&&twins[0].id!==twins[1].id&&firstWorks&&clickedQuality==='duplicate360');duplicate.remove();
    const hidden=document.createElement('button');hidden.className='explicit-resolution shaka-hidden';hidden.textContent='240p';fixture.querySelector('.shaka-resolutions').appendChild(hidden);
    assert('hidden source choices are not exposed as available resolutions',!read().quality.items.some(t=>t.label==='240p'));hidden.remove();
    for(const b of fixture.querySelectorAll('.explicit-resolution'))if(['480p','1080p'].includes(b.textContent))b.remove();
    menus=read();assert('only remaining web resolutions are shown, without phantom fallback qualities',menus.quality.items.map(t=>t.label).join(',')==='720p,360p,Otomatis');
    const extra=document.createElement('button');extra.className='explicit-resolution';extra.textContent='1440p';fixture.querySelector('.shaka-resolutions').appendChild(extra);
    assert('later web quality additions appear on the next snapshot',read().quality.items.some(t=>t.label==='1440p'));
    assert('all source subtitle languages plus Off are mirrored',menus.captionLanguage.items.map(t=>t.label).join(',')==='Indonesian,English,Japanese,Nonaktif');
    const supplied=document.createElement('button');supplied.setAttribute('role','menuitemradio');supplied.textContent='English (uploaded)';fixture.querySelector('.shaka-text-languages').appendChild(supplied);
    assert('an available track is not mistaken for an upload action because of its label',read().captionLanguage.items.some(t=>t.label==='English (uploaded)'));supplied.remove();
    assert('upload subtitle is excluded from the native tree',!JSON.stringify(menus).includes('Unggah')&&upload===0);
    assert('arbitrary/unlisted actions cannot click source controls',adapter.run('upload').ok===false&&upload===0);
    assert('subtitle root includes language size position and style',menus.subtitleHome.items.map(t=>t.submenu).join(',')==='captionLanguage,captionSize,captionPosition,captionStyle');
    for(const b of fixture.querySelectorAll('.shaka-caption-size-button,.shaka-caption-position-button,.nb-caption-style-overflow-button'))b.classList.add('shaka-hidden');
    assert('overflow navigation hiding does not remove available caption features',read().subtitleHome.items.length===4);
    const trigger=fixture.querySelector('.shaka-caption-size-button');trigger.disabled=true;
    assert('actually disabled caption features are omitted',!read().subtitleHome.items.some(t=>t.id==='captionSize'));trigger.disabled=false;
    assert('all seven real size options are present',menus.captionSize.items.length===7);
    assert('all ten real positions are present',menus.captionPosition.items.length===10);
    assert('style exposes source color opacity blur timing and reset',menus.captionStyle.items.length===5);
    assert('range limits and step come directly from web inputs',menus.captionOpacity.range.min===0&&menus.captionOpacity.range.max===1&&menus.captionOpacity.range.step===.05&&menus.captionBlur.range.max===12);
    const range=menus.captionOpacity.range;adapter.run(range.id,.6);menus=read();
    assert('range change dispatches source input/change events',Number(opacity.value)===.6&&inputEvents===1&&changeEvents===1&&menus.captionOpacity.range.formatted==='60%');
    assert('out-of-range and non-source steps are rejected',!adapter.run(range.id,1.2).ok&&!adapter.run(range.id,.57).ok);
    const red=menus.captionColor.items.find(t=>t.label==='Merah');adapter.run(red.id);assert('color uses the source swatch and selected state',read().captionColor.items.find(t=>t.label==='Merah').selected);
    adapter.run('web:captionDelay:1');assert('subtitle timing uses source stepper and live value',read().captionDelay.title.endsWith('0.5s'));
    adapter.run('web:captionStyle:reset');assert('reset invokes source reset action',resets===1);
    fixture.querySelector('.shaka-resolutions').replaceChildren();
    assert('an explicitly empty web quality menu does not gain invented fallback entries',read().quality.items.length===0);
    adapter.close(f);assert('closing source style panel uses its own close control',!fixture.querySelector('.player-caption-style-panel'));
    return {checks:checks.length,passed:checks,fixture:'Hidden browser DOM; actual player preferences untouched'};
  }finally{fixture.remove();}
})()
