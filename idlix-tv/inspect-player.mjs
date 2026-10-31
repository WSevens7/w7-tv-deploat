import fs from 'node:fs';
async function getTabs(){let last;for(let i=0;i<8;i++){try{return await (await fetch('http://127.0.0.1:9223/json')).json();}catch(e){last=e;await new Promise(ok=>setTimeout(ok,750));}}throw last;}
let tabs=await getTabs();
for(let n=0;n<5&&!tabs.some(t=>t.url?.startsWith('https://z2.idlixku.com'));n++){await new Promise(ok=>setTimeout(ok,1000));tabs=await (await fetch('http://127.0.0.1:9223/json')).json();}
const tab=tabs.find(t=>t.url?.startsWith('https://z2.idlixku.com'))||tabs.find(t=>t.type==='page');
if(!tab)throw Error('No app WebView');
const ws=new WebSocket(tab.webSocketDebuggerUrl);
await new Promise((ok,no)=>{ws.onopen=ok;ws.onerror=no;});
let seq=0;const waiting=new Map();const contexts=[];
ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.method==='Runtime.executionContextCreated')contexts.push(m.params.context);if(m.id&&waiting.has(m.id)){waiting.get(m.id)(m);waiting.delete(m.id);}};
function call(method,params={}){return new Promise((ok,no)=>{const id=++seq;const timer=setTimeout(()=>{waiting.delete(id);no(Error('Timeout '+method));},12000);waiting.set(id,r=>{clearTimeout(timer);r.error?no(Error(JSON.stringify(r.error))):ok(r.result);});ws.send(JSON.stringify({id,method,params}));});}
try{
await call('Runtime.enable');
if(process.argv[2]==='navigate'){
 await call('Page.enable');await call('Page.stopLoading');console.log(JSON.stringify(await call('Page.navigate',{url:process.argv[3]})));
}
else if(process.argv[2]==='click'){
 await call('Input.dispatchMouseEvent',{type:'mouseMoved',x:480,y:300});
 await new Promise(ok=>setTimeout(ok,300));
 const selector=JSON.stringify(process.argv[3]);
 const r=await call('Runtime.evaluate',{expression:`(()=>{const e=[...document.querySelectorAll(${selector})].find(e=>{const r=e.getBoundingClientRect();return r.width&&r.height});if(!e)return null;const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2};})()`,returnByValue:true});
 const p=r.result.value;if(!p)throw Error('Visible control not found');
 await call('Input.dispatchMouseEvent',{type:'mousePressed',...p,button:'left',clickCount:1});
 await call('Input.dispatchMouseEvent',{type:'mouseReleased',...p,button:'left',clickCount:1});
 console.log('Clicked visible control');
}
else if(process.argv[2]==='frames')console.log(JSON.stringify(await call('Page.getFrameTree'),null,2));
else if(process.argv[2]==='contexts')console.log(JSON.stringify(contexts.map(c=>({id:c.id,origin:c.origin,name:c.name,aux:c.auxData})),null,2));
else{
 const expression=fs.readFileSync(process.argv[2],'utf8');
 const params={expression,returnByValue:true,awaitPromise:true};if(process.argv[3])params.contextId=+process.argv[3];
 const r=await call('Runtime.evaluate',params);console.log(JSON.stringify(r.result?.value??r.exceptionDetails??r,null,2));
}
}finally{ws.close();}
