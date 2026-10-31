package com.w7.idlixwebtv;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Real Android Handler/remote integration test with a fixture media state, not a live stream. */
public final class SelectionTimeoutProbe extends Instrumentation {
    private MainActivity activity;
    private TvPlayer player;
    private int checks;
    private boolean episodesOnly;
    private int originalTimeout=10;
    private Field field(String name)throws Exception{Field f=TvPlayer.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private Object read(String name)throws Exception{Object[] value=new Object[1];runOnMainSync(()->{try{value[0]=field(name).get(player);}catch(Exception e){throw new RuntimeException(e);}});return value[0];}
    private void invoke(String name,Class<?>[] types,Object...args)throws Exception{Method m=TvPlayer.class.getDeclaredMethod(name,types);m.setAccessible(true);runOnMainSync(()->{try{m.invoke(player,args);}catch(Exception e){throw new RuntimeException(e);}});}
    private void key(int code){sendKeyDownUpSync(code);waitForIdleSync();}
    private void check(String name,boolean passed){if(!passed)throw new AssertionError(name);checks++;Bundle b=new Bundle();b.putString("check",name);sendStatus(1,b);}
    private void toPlayback()throws Exception{for(int i=0;i<6&&!((String)read("menu")).isEmpty();i++)key(KeyEvent.KEYCODE_BACK);for(int i=0;i<3&&(int)read("navigationLevel")>0;i++)key(KeyEvent.KEYCODE_DPAD_UP);}
    private void choose(int seconds)throws Exception{
        toPlayback();key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_RIGHT);key(KeyEvent.KEYCODE_DPAD_RIGHT);key(KeyEvent.KEYCODE_DPAD_CENTER);
        key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);for(int i=0;i<(seconds-5)/5;i++)key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);key(KeyEvent.KEYCODE_BACK);
        check("Duration "+seconds+" seconds chosen through remote and persisted",activity.getSharedPreferences("player",0).getInt("selection_timeout_seconds",0)==seconds);
    }
    private boolean selection()throws Exception{return (int)read("navigationLevel")==1&&(boolean)read("chrome")&&((String)read("menu")).isEmpty();}
    private boolean hidden()throws Exception{return (int)read("navigationLevel")==0&&!(boolean)read("chrome")&&((View)read("bottom")).getVisibility()==View.GONE;}
    @Override public void onCreate(Bundle args){super.onCreate(args);episodesOnly=args!=null&&"true".equals(args.getString("episodesOnly"));start();}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;
        try{
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            Field owner=MainActivity.class.getDeclaredField("player");owner.setAccessible(true);player=(TvPlayer)owner.get(activity);
            runOnMainSync(()->player.stop());
            JSONObject media=new JSONObject().put("paused",false).put("duration",600).put("time",40).put("rate",1).put("title","Pengujian auto-hide");
            runOnMainSync(()->{try{field("state").set(player,media);}catch(Exception e){throw new RuntimeException(e);}});
            originalTimeout=activity.getSharedPreferences("player",0).getInt("selection_timeout_seconds",10);
            check("Controller honors persisted timeout",(int)read("selectionTimeoutSeconds")==originalTimeout);
            invoke("setActive",new Class<?>[]{boolean.class},true);waitForIdleSync();
            if(!episodesOnly){
            choose(5);Thread.sleep(3500);check("5-second selection remains visible before expiry",selection());Thread.sleep(2200);check("5-second expiry hides controls and restores playback mode",hidden());
            runOnMainSync(()->{try{media.put("paused",true);}catch(Exception e){throw new RuntimeException(e);}});
            choose(10);Thread.sleep(3000);invoke("refresh",new Class<?>[]{});Thread.sleep(7700);check("10-second selection also expires while paused after state refresh",hidden());
            choose(15);Thread.sleep(15700);check("15-second selection expires",hidden());
            choose(5);Thread.sleep(3000);key(KeyEvent.KEYCODE_DPAD_LEFT);Thread.sleep(3000);check("Remote interaction restarts idle countdown",selection());Thread.sleep(2700);check("Selection expires five seconds after last interaction",hidden());
            choose(5);key(KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(5700);check("Editing a submenu is not interrupted by selection timeout",((String)read("menu")).equals("home"));
            }
            JSONArray episodes=new JSONArray();for(int n=1;n<=3;n++)episodes.put(new JSONObject().put("id","/series/fixture/season/1/episode/"+n).put("number",n).put("title","Episode "+n).put("current",n==2));
            media.put("episodes",episodes).put("season",1).put("contentType","series");
            choose(5);key(KeyEvent.KEYCODE_DPAD_DOWN);check("Second Down opens episode rail with the shared timeout",(int)read("navigationLevel")==2&&((View)read("episodeRail")).getVisibility()==View.VISIBLE);
            Thread.sleep(3000);key(KeyEvent.KEYCODE_DPAD_RIGHT);Thread.sleep(3000);check("Moving between episodes restarts idle deadline",(int)read("navigationLevel")==2);Thread.sleep(2700);
            check("Idle episode rail disappears and restores watching controls",hidden()&&((View)read("episodeRail")).getVisibility()==View.GONE);
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);check("Episodes can be reopened after idle expiry",(int)read("navigationLevel")==2);
            key(KeyEvent.KEYCODE_DPAD_CENTER);check("Selecting current episode leaves no pending rail timeout",(int)read("navigationLevel")==0&&!((Handler)read("handler")).hasCallbacks((Runnable)read("hideSelection")));
            choose(originalTimeout);toPlayback();check("Original timeout preference restored",activity.getSharedPreferences("player",0).getInt("selection_timeout_seconds",0)==originalTimeout);
            result.putInt("checks",checks);result.putString("result","PASS; real-clock Android fixture; no live source media used");code=Activity.RESULT_OK;
        }catch(Throwable error){result.putInt("checks",checks);result.putString("failure",error.toString());}
        finally{if(activity!=null)runOnMainSync(()->{activity.getSharedPreferences("player",0).edit().putInt("selection_timeout_seconds",originalTimeout).commit();activity.finish();});}
        finish(code,result);
    }
}
