(function(){
  if(window.__w7WebMenus)return;
  const actions=new Map();
  const triggers={quality:'.shaka-resolution-button',captionLanguage:'.shaka-caption-button',captionSize:'.shaka-caption-size-button',captionPosition:'.shaka-caption-position-button',captionStyle:'.nb-caption-style-overflow-button'};
  const styleMenuNames=new Set(['captionStyle','captionColor','captionOpacity','captionBlur','captionDelay']);
  function label(el){
    if(!el)return '';
    const copy=el.cloneNode(true);copy.querySelectorAll('svg,i,.material-icons').forEach(n=>n.remove());
    return (copy.textContent||'').replace(/\s+/g,' ').trim();
  }
  function root(f){return f?.v?.closest('.hero-player-playing');}
  function enabled(el){return !!el&&!el.disabled&&!el.hidden&&el.getAttribute('aria-disabled')!=='true';}
  function chosen(el){return el.getAttribute('aria-checked')==='true'||el.getAttribute('aria-pressed')==='true'||!!el.querySelector('.shaka-chosen-item');}
  function captionPanel(f){return f.c?.querySelector('.player-caption-style-panel');}
  function buttonItems(menu,buttons,fallback=[]){
    const seen=new Map();
    return [...buttons].filter(b=>enabled(b)&&!b.classList.contains('shaka-hidden')&&!b.classList.contains('shaka-back-to-overflow-button')).map(b=>{
      const text=label(b)||b.getAttribute('aria-label')||'';
      if(!text||(b.getAttribute('role')!=='menuitemradio'&&/upload|unggah/i.test(text+' '+b.className)))return null;
      const matches=fallback.filter(t=>t.label===text),occurrence=seen.get(text)||0;seen.set(text,occurrence+1);
      const source=matches[occurrence];
      const id='web:'+menu+':'+(source?source.id:encodeURIComponent(text)+':'+occurrence);
      actions.set(id,{type:'button',el:b,menu});
      return {id,label:text,selected:chosen(b)};
    }).filter(Boolean);
  }
  function textMenu(f,kind){
    const r=root(f),trigger=r?.querySelector(triggers[kind]);
    const title=trigger?.getAttribute('aria-label');
    if(!title)return null;
    return [...r.querySelectorAll('.shaka-sub-menu')].find(m=>label(m.querySelector('.shaka-back-to-overflow-button'))===title)||null;
  }
  function prepare(f,kind){
    const r=root(f);if(!r)return false;
    const style=styleMenuNames.has(kind);
    if(style&&captionPanel(f))return true;
    const trigger=r.querySelector(triggers[style?'captionStyle':kind]);
    if(!enabled(trigger))return false;
    trigger.click();return true;
  }
  function read(f,fallback){
    actions.clear();const r=root(f);if(!r?.querySelectorAll)return {};
    const menus={};
    const q=r.querySelector('.shaka-resolutions');
    if(q){const items=buttonItems('quality',q.querySelectorAll('button.explicit-resolution,button.shaka-enable-abr-button'),fallback.qualities);menus.quality={title:'Kualitas video',items};}
    const languages=r.querySelector('.shaka-text-languages');
    if(languages){const items=buttonItems('captionLanguage',languages.querySelectorAll('button'),fallback.subtitles);menus.captionLanguage={title:'Bahasa subtitle',items};}
    if(!menus.captionLanguage)menus.captionLanguage={title:'Bahasa subtitle',items:fallback.subtitles.map(t=>({...t,command:'subtitle'}))};
    const subtitleItems=[{id:'captionLanguage',label:'Bahasa subtitle',summary:fallback.subtitles.find(t=>t.selected)?.label||'',submenu:'captionLanguage'}];
    for(const kind of ['captionSize','captionPosition']){
      const trigger=r.querySelector(triggers[kind]);if(!enabled(trigger))continue;
      const title=trigger.getAttribute('aria-label')||label(trigger),pane=textMenu(f,kind);
      menus[kind]={title,items:pane?buttonItems(kind,pane.querySelectorAll('button')):[]};
      subtitleItems.push({id:kind,label:title,summary:label(trigger.querySelector('.shaka-current-selection-span')),submenu:kind});
    }
    const styleTrigger=r.querySelector(triggers.captionStyle);
    if(enabled(styleTrigger)){
      const title=styleTrigger.getAttribute('aria-label')||'Gaya subtitle';
      subtitleItems.push({id:'captionStyle',label:title,summary:label(styleTrigger.querySelector('.shaka-current-selection-span')),submenu:'captionStyle'});
      const panel=captionPanel(f),items=[];
      menus.captionStyle={title,items};
      if(panel){
        const colors=panel.querySelectorAll('button.player-caption-style-panel__swatch');
        if(colors.length){
          const group=colors[0].closest('.player-caption-style-panel__row'),name=label(group?.querySelector('label'))||'Warna';
          const colorItems=[...colors].filter(enabled).map((b,i)=>{const id='web:captionColor:'+i;actions.set(id,{type:'button',menu:'captionColor',el:b});return {id,label:b.getAttribute('aria-label')||label(b),selected:chosen(b)};});
          menus.captionColor={title:name,items:colorItems};items.push({id:'captionColor',label:name,summary:colorItems.find(t=>t.selected)?.label||'',submenu:'captionColor'});
        }
        for(const [kind,inputId] of [['captionOpacity','caption-bg-opacity'],['captionBlur','caption-bg-blur']]){
          const input=panel.querySelector('input#'+inputId);if(!enabled(input)||input.type!=='range')continue;
          const row=input.closest('.player-caption-style-panel__row'),name=label(row?.querySelector('label'))||inputId;
          const min=Number(input.min),max=Number(input.max),step=Number(input.step),value=Number(input.value);
          if(![min,max,step,value].every(Number.isFinite)||max<=min||step<=0)continue;
          const id='web:'+kind;actions.set(id,{type:'range',menu:kind,el:input,min,max,step});
          const formatted=label(row?.querySelector('.player-caption-style-panel__value'))||String(value);
          menus[kind]={title:name,kind:'range',range:{id,min,max,step,value,formatted},items:[]};
          items.push({id:kind,label:name,summary:formatted,submenu:kind});
        }
        const timing=panel.querySelector('.player-caption-style-panel__latency');
        if(timing){
          const title=label(timing.querySelector('.player-caption-style-panel__latency-header'))||'Jeda waktu';
          const summary=label(timing.querySelector('.player-caption-style-panel__latency-value'));
          const steps=[...timing.querySelectorAll('button.player-caption-style-panel__stepper')].filter(enabled).map((b,i)=>{const id='web:captionDelay:'+i;actions.set(id,{type:'button',menu:'captionDelay',el:b});return {id,label:b.getAttribute('aria-label')||label(b),stay:true};});
          menus.captionDelay={title:title+' · '+summary,items:steps};items.push({id:'captionDelay',label:title,summary,submenu:'captionDelay'});
        }
        const reset=panel.querySelector('button.player-caption-style-panel__reset');
        if(enabled(reset)){const id='web:captionStyle:reset';actions.set(id,{type:'button',menu:'captionStyle',el:reset});items.push({id,label:label(reset),stay:true});}
      }
    }
    menus.subtitleHome={title:'Subtitle',items:subtitleItems};
    return menus;
  }
  function run(id,value){
    const action=actions.get(id);if(!action||!action.el.isConnected||!enabled(action.el))return {ok:false,error:'Pilihan web berubah. Buka kembali menunya.'};
    if(action.type==='range'){
      const n=Number(value),index=(n-action.min)/action.step;
      if(!Number.isFinite(n)||n<action.min-1e-6||n>action.max+1e-6||Math.abs(index-Math.round(index))>1e-5)return {ok:false,error:'Nilai di luar pilihan web.'};
      const setter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value')?.set;
      if(setter)setter.call(action.el,String(n));else action.el.value=String(n);
      action.el.dispatchEvent(new Event('input',{bubbles:true}));action.el.dispatchEvent(new Event('change',{bubbles:true}));
    }else action.el.click();
    return {ok:true};
  }
  function close(f){const panel=captionPanel(f);panel?.querySelector('button.player-caption-style-panel__icon')?.click();actions.clear();}
  window.__w7WebMenus={read,prepare,run,close};
})();
