package com.w7.idlixwebtv;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.Locale;

/** Native TV controls; media, subtitles and adaptive tracks remain owned by the website. */
final class TvPlayer {
    interface Listener { void modeChanged(boolean active); }
    private final Activity activity;
    private final WebView web;
    private final Listener listener;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final FrameLayout overlay,top,bottom;
    private final LinearLayout sheet;
    private final PlayerOptions options;
    private final EpisodeRail episodeRail;
    private final TextView title,time,remaining,hint;
    private final ProgressBar buffering;
    private final SeekBar timeline;
    private final ImageButton play;
    private final String adapter;
    private final VideoGlass glass;
    private JSONObject state=new JSONObject();
    private boolean active,running,inflight,chrome=true,suppressed,dragging,failed,lastPaused;
    private String menu="";
    private long lastSeek;
    private int navigationLevel,selectedSetting;
    private String pendingEpisode="",lastEpisode="";
    private long pendingUntil;
    private long nextGlassCapture;
    private double lastGlassTime=-100;
    private boolean sampling;
    private String railSignature="";
    private String choiceSignature="";
    private String webMenuSignature="";
    private TextView rangeValue;
    private SeekBar rangeSlider;
    private JSONObject rangeSpec;
    private LinearLayout menuList;
    private int menuFocusIndex;
    private int selectionTimeoutSeconds;
    private final Runnable tick=this::poll;
    private final Runnable hide=()->{if(active&&navigationLevel==0&&menu.isEmpty()&&!state.optBoolean("paused",true))showChrome(false);};
    private final Runnable hideSelection=this::hideIdleSelection;

    TvPlayer(Activity a,WebView w,FrameLayout parent,Listener l){
        activity=a;web=w;listener=l;adapter=readAdapter();glass=new VideoGlass(a);
        selectionTimeoutSeconds=a.getSharedPreferences("player",0).getInt("selection_timeout_seconds",10);if(selectionTimeoutSeconds!=5&&selectionTimeoutSeconds!=10&&selectionTimeoutSeconds!=15)selectionTimeoutSeconds=10;
        overlay=new FrameLayout(a);overlay.setFocusable(true);overlay.setVisibility(View.GONE);
        parent.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        top=new FrameLayout(a);top.setBackgroundColor(Color.TRANSPARENT);
        overlay.addView(top,new FrameLayout.LayoutParams(-1,dp(112),Gravity.TOP));
        ImageButton close=icon(R.drawable.ic_arrow_back,"Tutup pemutar",this::closePlayer);
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.START);cp.leftMargin=dp(26);cp.topMargin=dp(22);top.addView(close,cp);
        title=text("",21);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setMaxLines(2);title.setMaxWidth(a.getResources().getDisplayMetrics().widthPixels-dp(136));title.setPadding(dp(4),dp(10),dp(4),dp(10));
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP);tp.leftMargin=dp(88);tp.rightMargin=dp(48);tp.topMargin=dp(22);top.addView(title,tp);
        bottom=new FrameLayout(a);
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0x28000000,0x99000000}));
        FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(-1,dp(128),Gravity.BOTTOM);overlay.addView(bottom,blp);
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(36),dp(12),dp(36),dp(18));bottom.addView(content,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout times=new LinearLayout(a);time=text("0:00",14);remaining=text("",14);remaining.setGravity(Gravity.END);
        times.addView(time,new LinearLayout.LayoutParams(0,-2,1));times.addView(remaining,new LinearLayout.LayoutParams(0,-2,1));content.addView(times);
        timeline=new SeekBar(a);timeline.setMax(10000);timeline.setFocusable(false);timeline.setPadding(0,0,0,0);
        timeline.setProgressTintList(ColorStateList.valueOf(0xffd1eeff));timeline.setSecondaryProgressTintList(ColorStateList.valueOf(0x9989a9be));timeline.setProgressBackgroundTintList(ColorStateList.valueOf(0x55586878));timeline.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){dragging=true;handler.removeCallbacks(hide);}
            public void onProgressChanged(SeekBar b,int value,boolean user){if(user)time.setText(format(state.optDouble("duration")*value/10000));}
            public void onStopTrackingTouch(SeekBar b){command("position",state.optDouble("duration")*b.getProgress()/10000);dragging=false;wake();}
        });content.addView(timeline,new LinearLayout.LayoutParams(-1,dp(24)));
        LinearLayout row=new LinearLayout(a);row.setBaselineAligned(false);row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(icon(R.drawable.ic_replay_10,"Mundur 10 detik",()->seek(-10)),new LinearLayout.LayoutParams(dp(40),dp(44)));
        play=icon(R.drawable.ic_play_arrow,"Putar / jeda",()->{command("toggle",null);wake();});
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(44),dp(48));pp.leftMargin=dp(8);pp.rightMargin=dp(8);row.addView(play,pp);
        row.addView(icon(R.drawable.ic_forward_10,"Maju 10 detik",()->seek(10)),new LinearLayout.LayoutParams(dp(40),dp(44)));
        hint=text("↓ Pengaturan · OK Putar/jeda",12);hint.setTextColor(0xffdddddd);hint.setPadding(dp(14),0,dp(12),0);row.addView(hint,new LinearLayout.LayoutParams(0,-2,1));
        options=new PlayerOptions(a,glass,new PlayerOptions.Listener(){
            public void selected(int index){selectedSetting=index;armSelectionHide();}
            public void open(int index){openSetting(index);}
        });
        row.addView(options,new LinearLayout.LayoutParams(dp(360),dp(54)));content.addView(row);
        episodeRail=new EpisodeRail(a,glass,e->{if(e.optBoolean("current")){enterPlayback();return;}pendingEpisode=e.optString("id");pendingUntil=SystemClock.uptimeMillis()+120000;command(e.optString("kind").equals("movie")?"movie":"episode",pendingEpisode);setActive(false);});
        FrameLayout.LayoutParams rlp=new FrameLayout.LayoutParams(-1,dp(265),Gravity.BOTTOM);rlp.leftMargin=dp(24);rlp.rightMargin=dp(24);rlp.bottomMargin=dp(18);overlay.addView(episodeRail,rlp);
        buffering=new ProgressBar(a);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.CENTER);overlay.addView(buffering,bp);buffering.setVisibility(View.GONE);
        sheet=new LinearLayout(a);sheet.setOrientation(LinearLayout.VERTICAL);sheet.setPadding(dp(20),dp(18),dp(20),dp(18));sheet.setBackground(glass.surface(sheet,24,false));
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(dp(300),-2,Gravity.BOTTOM|Gravity.END);sp.bottomMargin=dp(104);sp.rightMargin=dp(36);overlay.addView(sheet,sp);sheet.setVisibility(View.GONE);
    }
    private String readAdapter(){try(ByteArrayOutputStream b=new ByteArrayOutputStream()){for(String file:new String[]{"web-menu-adapter.js","tv-player.js"}){try(InputStream in=activity.getAssets().open(file)){byte[] data=new byte[8192];int n;while((n=in.read(data))>0)b.write(data,0,n);b.write('\n');}}return b.toString("UTF-8");}catch(IOException e){return "";}}
    boolean isActive(){return active;}
    void start(){if(running)return;running=true;glass.start();armSelectionHide();poll();}
    void stop(){running=false;glass.stop();handler.removeCallbacks(tick);handler.removeCallbacks(hide);handler.removeCallbacks(hideSelection);}
    void destroy(){stop();episodeRail.destroy();glass.destroy();web.evaluateJavascript("window.__w7TV&&window.__w7TV.command('release')",null);}
    void bringToFront(){overlay.bringToFront();}
    void enable(){suppressed=false;failed=false;wake();if(!inflight)poll();}
    private boolean allowed(){try{String host=android.net.Uri.parse(web.getUrl()).getHost();return host!=null&&(host.equals("idlixku.com")||host.endsWith(".idlixku.com"));}catch(Exception e){return false;}}
    private void poll(){
        handler.removeCallbacks(tick);if(!running||inflight)return;
        if(!allowed()||adapter.isEmpty()){setActive(false);handler.postDelayed(tick,900);return;}
        inflight=true;
        web.evaluateJavascript(adapter+"\nJSON.stringify(window.__w7TV.poll("+(!suppressed)+","+(chrome?(navigationLevel==2?305:132):0)+"))",result->{
            inflight=false;if(!running)return;
            try{JSONObject next=decode(result);boolean available=next.optBoolean("available");if(!available)suppressed=false;
                if(!pendingEpisode.isEmpty()){if(pendingEpisode.equals(next.optString("contentId",next.optString("episodeId")))||SystemClock.uptimeMillis()>pendingUntil)pendingEpisode="";else available=false;}
                state=next;setActive(available&&!suppressed);if(active){refresh();sampleGlass();}String error=next.optString("error");if(!error.isEmpty())toast(error);failed=false;}
            catch(Exception e){if(!failed){failed=true;web.evaluateJavascript("window.__w7TV&&window.__w7TV.command('release')",null);setActive(false);}}
            handler.postDelayed(tick,650);
        });
    }
    private JSONObject decode(String raw)throws JSONException{Object value=new JSONTokener(raw==null?"null":raw).nextValue();return value instanceof JSONObject?(JSONObject)value:new JSONObject(String.valueOf(value));}
    private void setActive(boolean value){if(active==value)return;handler.removeCallbacks(hideSelection);active=value;overlay.setVisibility(value?View.VISIBLE:View.GONE);menu="";navigationLevel=0;sheet.setVisibility(View.GONE);episodeRail.setVisibility(View.GONE);listener.modeChanged(value);if(value){nextGlassCapture=0;lastGlassTime=-100;bringToFront();overlay.requestFocus();wake();}else handler.removeCallbacks(hide);}
    private void refresh(){
        String episode=state.optString("contentId",state.optString("episodeId"));if(!lastEpisode.isEmpty()&&!lastEpisode.equals(episode)){lastGlassTime=-100;if(navigationLevel!=0)enterPlayback();}lastEpisode=episode;
        updateText(title,state.optString("title",""));double duration=state.optDouble("duration",0),now=state.optDouble("time",0);
        if(!dragging){updateText(time,format(now));updateText(remaining,"−"+format(Math.max(0,duration-now))+"   /   "+format(duration));timeline.setProgress(duration>0?(int)(now/duration*10000):0);}
        timeline.setSecondaryProgress(duration>0?(int)(Math.min(duration,state.optDouble("buffered"))/duration*10000):0);
        boolean paused=state.optBoolean("paused",true);int image=paused?R.drawable.ic_play_arrow:R.drawable.ic_pause;if(!Integer.valueOf(image).equals(play.getTag())){play.setImageResource(image);play.setTag(image);play.setContentDescription(paused?"Putar":"Jeda");}buffering.setVisibility(state.optBoolean("waiting")?View.VISIBLE:View.GONE);
        if(paused){handler.removeCallbacks(hide);if(!lastPaused&&menu.isEmpty()&&navigationLevel==0)showChrome(true);}lastPaused=paused;
        options.describe(state.optString("quality","Otomatis"),state.optString("subtitle","Nonaktif"));
        JSONArray episodes=state.optJSONArray("episodes");int count=episodes==null?0:episodes.length();boolean movie=state.optString("contentType").equals("movie");
        if(navigationLevel==1)updateText(hint,movie?"← / → Pilih · ↓ Rekomendasi":count>0?"← / → Pilih · ↓ Episode":"← / → Pilih · OK Buka");
        if(navigationLevel==2&&movie){String signature=recommendationSignature();if(!signature.equals(railSignature))renderRecommendations();}
        if((menu.equals("quality")||menu.equals("subtitle"))&&!choiceSignature.equals(choiceSignature(menu)))choices(menu);
        if(menu.startsWith("web:")){String key=menu.substring(4);JSONObject data=webMenuData(key);if(rangeSlider!=null&&data.optString("kind").equals("range")){rangeSpec=data.optJSONObject("range");syncRange();}else if(!webMenuSignature.equals(data.toString()))renderWebMenu(key);}
    }
    private void command(String name,Object arg){
        web.evaluateJavascript("window.__w7TV?JSON.stringify(window.__w7TV.command("+JSONObject.quote(name)+","+(arg==null?"null":arg instanceof Number||arg instanceof JSONObject?String.valueOf(arg):JSONObject.quote(String.valueOf(arg)))+")):null",r->{try{JSONObject result=decode(r);if(!result.optBoolean("ok"))toast(result.optString("error","Kontrol belum siap."));}catch(Exception ignored){}});
    }
    private void seek(int seconds){long now=SystemClock.uptimeMillis();if(now-lastSeek<180)return;lastSeek=now;command("seek",seconds);wake();hint.setText(seconds>0?"Maju 10 detik":"Mundur 10 detik");}
    private void showChrome(boolean show){chrome=show;top.setVisibility(show||navigationLevel>0?View.VISIBLE:View.GONE);bottom.setVisibility(show&&navigationLevel<2?View.VISIBLE:View.GONE);}
    private void wake(){showChrome(true);handler.removeCallbacks(hide);handler.postDelayed(hide,4500);}
    private void armSelectionHide(){handler.removeCallbacks(hideSelection);if(active&&(navigationLevel==1||navigationLevel==2)&&menu.isEmpty())handler.postDelayed(hideSelection,selectionTimeoutSeconds*1000L);}
    private void hideIdleSelection(){if(active&&(navigationLevel==1||navigationLevel==2)&&menu.isEmpty()){navigationLevel=0;episodeRail.setVisibility(View.GONE);overlay.requestFocus();hint.setText("↓ Pengaturan · OK Putar/jeda");showChrome(false);}}
    private void closePlayer(){command("close",null);setActive(false);}
    private void fallback(){suppressed=true;command("release",null);setActive(false);toast("Kontrol web aktif. Menu → Player TV untuk kembali.");}
    private void panel(String heading){
        handler.removeCallbacks(hideSelection);menuList=null;menuFocusIndex=0;
        handler.removeCallbacks(hide);episodeRail.setVisibility(View.GONE);showChrome(true);sheet.setVisibility(View.VISIBLE);sheet.removeAllViews();
        TextView h=text(heading,23);h.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));h.setPadding(dp(4),dp(4),0,dp(16));sheet.addView(h);
        TextView note=text("↑ / ↓ Pilih     OK Terapkan     Back Kembali",11);note.setTextColor(0xffaaaaaa);note.setPadding(dp(4),0,0,dp(12));sheet.addView(note);
    }
    private LinearLayout list(){ScrollView scroll=new ScrollView(activity){
        @Override protected void onMeasure(int w,int h){int cap=Math.min(dp(280),activity.getResources().getDisplayMetrics().heightPixels-dp(220));if(MeasureSpec.getMode(h)!=MeasureSpec.UNSPECIFIED)cap=Math.min(cap,MeasureSpec.getSize(h));super.onMeasure(w,MeasureSpec.makeMeasureSpec(Math.max(dp(52),cap),MeasureSpec.AT_MOST));}
    };scroll.setFillViewport(false);LinearLayout list=new LinearLayout(activity);menuList=list;list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);sheet.addView(scroll,new LinearLayout.LayoutParams(-1,-2));return list;}
    private Button add(LinearLayout list,String label,boolean selected,Runnable action){Button b=button((selected?"✓  ":"")+label,action);b.setSelected(selected);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setPadding(dp(14),0,dp(10),0);b.setTextSize(16);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.bottomMargin=dp(8);list.addView(b,lp);return b;}
    private void focusFirst(LinearLayout list){focusList(list,null);}
    private void focusList(LinearLayout list,Object preferred){for(int i=0;i<list.getChildCount();i++)list.getChildAt(i).setId(View.generateViewId());for(int i=0;i<list.getChildCount();i++){final int index=i;View view=list.getChildAt(i);view.setNextFocusUpId(list.getChildAt(Math.max(0,i-1)).getId());view.setNextFocusDownId(list.getChildAt(Math.min(list.getChildCount()-1,i+1)).getId());view.setNextFocusRightId(view.getId());view.setOnFocusChangeListener((v,on)->{if(on&&menuList==list)markMenuFocus(index);});}list.post(()->{if(menuList!=list)return;for(int i=0;i<list.getChildCount();i++){View view=list.getChildAt(i);if(preferred!=null&&preferred.equals(view.getTag())){focusMenuIndex(i);return;}}if(list.getChildCount()>0)focusMenuIndex(0);});}
    private void selectSettings(){enterSettings(selectedSetting);}
    private void openSetting(int index){selectedSetting=Math.max(0,Math.min(2,index));navigationLevel=1;if(selectedSetting==2)categories();else openWebMenu(selectedSetting==0?"quality":"subtitleHome");}
    private void enterSettings(int index){if(!active)return;navigationLevel=1;menu="";sheet.setVisibility(View.GONE);episodeRail.setVisibility(View.GONE);handler.removeCallbacks(hide);showChrome(true);options.focus(index);armSelectionHide();}
    private void enterPlayback(){handler.removeCallbacks(hideSelection);navigationLevel=0;menu="";sheet.setVisibility(View.GONE);episodeRail.setVisibility(View.GONE);overlay.requestFocus();hint.setText("↓ Pengaturan · OK Putar/jeda");wake();}
    private void enterEpisodes(){boolean movie=state.optString("contentType").equals("movie");JSONArray episodes=state.optJSONArray("episodes");if(!movie&&(episodes==null||episodes.length()==0))return;handler.removeCallbacks(hideSelection);navigationLevel=2;handler.removeCallbacks(hide);showChrome(true);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)episodeRail.getLayoutParams();lp.height=dp(movie?285:265);episodeRail.setLayoutParams(lp);if(movie)renderRecommendations();else episodeRail.open(episodes,state.optInt("season",1));armSelectionHide();}
    private String recommendationSignature(){return state.optString("recommendationState")+state.optJSONArray("recommendations");}
    private void renderRecommendations(){railSignature=recommendationSignature();JSONArray items=state.optJSONArray("recommendations");if(items==null||state.optString("recommendationState").equals("loading"))items=new JSONArray();episodeRail.openMovies(items,state.optString("recommendationGenre"),state.optString("recommendationState"));}
    private void sampleGlass(){long now=SystemClock.uptimeMillis();double media=state.optDouble("time",0);if(sampling||(!chrome&&menu.isEmpty())||now<nextGlassCapture||(glass.hasFrame()&&Math.abs(media-lastGlassTime)<.1))return;sampling=true;nextGlassCapture=now+1500;lastGlassTime=media;web.evaluateJavascript("window.__w7TV?window.__w7TV.captureFrame():null",raw->{sampling=false;if(!running||!active)return;try{Object value=new JSONTokener(raw).nextValue();if(value instanceof String)glass.offer((String)value);}catch(Exception ignored){}});}
    private void categories(){if(!active)return;menu="home";panel("Lainnya");LinearLayout list=list();
        add(list,"Kecepatan · "+rate(state.optDouble("rate",1)),false,()->choices("rate"));add(list,"Auto-hide pilihan · "+selectionTimeoutSeconds+" detik",false,()->choices("timeout"));add(list,"Gunakan kontrol web",false,this::fallback);add(list,"Tutup pemutar",false,this::closePlayer);focusFirst(list);
    }
    private JSONObject webMenuData(String key){
        JSONObject all=state.optJSONObject("webMenus"),data=all==null?null:all.optJSONObject(key);if(data!=null)return data;
        data=new JSONObject();try{data.put("title",key.equals("quality")?"Kualitas video":"Subtitle");
            if(key.equals("quality"))data.put("items",state.optJSONArray("qualities"));
            else if(key.equals("captionLanguage"))data.put("items",state.optJSONArray("subtitles"));
            else if(key.equals("subtitleHome"))data.put("items",new JSONArray().put(new JSONObject().put("id","captionLanguage").put("label","Bahasa subtitle").put("submenu","captionLanguage")));
        }catch(JSONException ignored){}return data;
    }
    private void openWebMenu(String key){if(!active)return;if(!key.equals("subtitleHome"))command("prepareMenu",key);renderWebMenu(key);}
    private void renderWebMenu(String key){
        Object preferred=menu.equals("web:"+key)&&menuList!=null&&menuFocusIndex<menuList.getChildCount()?menuList.getChildAt(menuFocusIndex).getTag():null;
        JSONObject data=webMenuData(key);webMenuSignature=data.toString();menu="web:"+key;rangeValue=null;rangeSlider=null;rangeSpec=null;
        panel(data.optString("title","Subtitle"));
        if(data.optString("kind").equals("range")&&data.optJSONObject("range")!=null){
            ((TextView)sheet.getChildAt(1)).setText("← / → Atur     Back Kembali");
            rangeSpec=data.optJSONObject("range");rangeValue=text("",26);rangeValue.setGravity(Gravity.CENTER);sheet.addView(rangeValue,new LinearLayout.LayoutParams(-1,dp(48)));
            rangeSlider=new SeekBar(activity);rangeSlider.setPadding(dp(10),0,dp(10),0);rangeSlider.setFocusable(true);rangeSlider.setKeyProgressIncrement(1);rangeSlider.setProgressTintList(ColorStateList.valueOf(0xffd1eeff));rangeSlider.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
            rangeSlider.setBackground(glass.surface(rangeSlider,16,true));rangeSlider.setActivated(true);sheet.addView(rangeSlider,new LinearLayout.LayoutParams(-1,dp(58)));
            rangeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean user){if(user)applyRange(p);}});
            syncRange();rangeSlider.post(rangeSlider::requestFocus);return;
        }
        LinearLayout list=list();JSONArray items=data.optJSONArray("items");
        if(items!=null)for(int i=0;i<items.length();i++){
            JSONObject item=items.optJSONObject(i);if(item==null)continue;
            String label=item.optString("label"),summary=item.optString("summary");
            add(list,label+(summary.isEmpty()?"":"\n"+summary),item.optBoolean("selected"),()->chooseWebItem(key,item)).setTag(item.optString("id"));
        }
        if(list.getChildCount()==0){Button waiting=add(list,"Menunggu pilihan dari web…",false,()->{});waiting.setEnabled(false);waiting.setFocusable(false);}
        focusList(list,preferred);
    }
    private void chooseWebItem(String key,JSONObject item){
        String submenu=item.optString("submenu");if(!submenu.isEmpty()){openWebMenu(submenu);return;}
        String id=item.optString("id");
        if(!id.startsWith("web:")&&(key.equals("quality")||key.equals("captionLanguage")))command(key.equals("quality")?"quality":"subtitle",id);
        else try{command("webAction",new JSONObject().put("id",id));}catch(JSONException ignored){}
        if(!item.optBoolean("stay"))backWebMenu(key);
    }
    private void backWebMenu(String key){
        if(key.equals("quality")||key.equals("subtitleHome")){enterSettings(selectedSetting);return;}
        openWebMenu(key.equals("captionColor")||key.equals("captionOpacity")||key.equals("captionBlur")||key.equals("captionDelay")?"captionStyle":"subtitleHome");
    }
    private void syncRange(){
        if(rangeSpec==null||rangeSlider==null)return;
        double min=rangeSpec.optDouble("min"),max=rangeSpec.optDouble("max"),step=rangeSpec.optDouble("step",1);
        int steps=(int)Math.max(1,Math.min(1000000,Math.round((max-min)/step)));rangeSlider.setMax(steps);
        rangeSlider.setProgress((int)Math.round((rangeSpec.optDouble("value")-min)/step));rangeValue.setText(rangeSpec.optString("formatted"));
    }
    private void applyRange(int progress){
        if(rangeSpec==null)return;double value=Math.min(rangeSpec.optDouble("max"),rangeSpec.optDouble("min")+progress*rangeSpec.optDouble("step",1));value=Math.round(value*1000000d)/1000000d;
        try{command("webAction",new JSONObject().put("id",rangeSpec.optString("id")).put("value",value));}catch(JSONException ignored){}
    }
    private void markMenuFocus(int index){if(menuList==null||menuList.getChildCount()==0)return;menuFocusIndex=Math.max(0,Math.min(menuList.getChildCount()-1,index));for(int i=0;i<menuList.getChildCount();i++)menuList.getChildAt(i).setActivated(i==menuFocusIndex);}
    private void focusMenuIndex(int index){markMenuFocus(index);if(menuList!=null&&menuList.getChildCount()>0)menuList.getChildAt(menuFocusIndex).requestFocus();}
    private void moveMenuFocus(int direction){focusMenuIndex(menuFocusIndex+direction);}
    private void clickMenuFocus(){if(menuList==null||menuList.getChildCount()==0)return;View view=menuList.getChildAt(menuFocusIndex);if(view.isEnabled())view.performClick();}
    private String choiceSignature(String kind){return String.valueOf(state.optJSONArray(kind.equals("subtitle")?"subtitles":"qualities"));}
    private void choices(String kind){View focused=activity.getCurrentFocus();Object preferred=kind.equals(menu)&&focused!=null?focused.getTag():null;menu=kind;choiceSignature=choiceSignature(kind);panel(kind.equals("timeout")?"Auto-hide pilihan":kind.equals("subtitle")?"Subtitle":kind.equals("quality")?"Kualitas video":"Kecepatan putar");LinearLayout list=list();
        if(kind.equals("timeout")){for(int seconds:new int[]{5,10,15})add(list,seconds+" detik",selectionTimeoutSeconds==seconds,()->{selectionTimeoutSeconds=seconds;activity.getSharedPreferences("player",0).edit().putInt("selection_timeout_seconds",seconds).apply();categories();});}
        else if(kind.equals("rate")){for(double r:new double[]{.5,.75,1,1.25,1.5,2})add(list,rate(r),Math.abs(state.optDouble("rate",1)-r)<.01,()->{command("rate",r);closePanel();});}
        else{JSONArray options=state.optJSONArray(kind.equals("subtitle")?"subtitles":"qualities");if(options!=null)for(int i=0;i<options.length();i++){JSONObject option=options.optJSONObject(i);if(option==null)continue;String id=option.optString("id"),label=option.optString("label");add(list,label,option.optBoolean("selected"),()->{command(kind,id);toast((kind.equals("subtitle")?"Subtitle: ":"Kualitas: ")+label);closePanel();}).setTag(id);}}
        if(list.getChildCount()==0)add(list,"Belum tersedia · Kembali",false,this::categories);focusList(list,preferred);
    }
    private void closePanel(){if(menu.startsWith("web:"))backWebMenu(menu.substring(4));else if(menu.equals("rate")||menu.equals("timeout"))categories();else enterSettings(selectedSetting);}
    boolean handle(KeyEvent e){
        int k=e.getKeyCode();boolean down=e.getAction()==KeyEvent.ACTION_DOWN;
        if((navigationLevel==1||navigationLevel==2)&&menu.isEmpty())armSelectionHide();
        // Selection owns remote input even if Android/WebView briefly loses native focus.
        if((navigationLevel>0||!menu.isEmpty())&&(k==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE||k==KeyEvent.KEYCODE_MEDIA_PLAY||k==KeyEvent.KEYCODE_MEDIA_PAUSE||k==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD||k==KeyEvent.KEYCODE_MEDIA_REWIND||k==KeyEvent.KEYCODE_MEDIA_NEXT||k==KeyEvent.KEYCODE_MEDIA_PREVIOUS||k==KeyEvent.KEYCODE_SPACE))return true;
        if(!menu.isEmpty()){
            if(menu.startsWith("web:")&&rangeSlider!=null&&webMenuData(menu.substring(4)).optString("kind").equals("range")&&(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT)){if(down){rangeSlider.setProgress(Math.max(0,Math.min(rangeSlider.getMax(),rangeSlider.getProgress()+(k==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1))));applyRange(rangeSlider.getProgress());}return true;}
            if(k==KeyEvent.KEYCODE_BACK||k==KeyEvent.KEYCODE_DPAD_LEFT){if(!down)closePanel();return true;}
            if(k==KeyEvent.KEYCODE_DPAD_RIGHT)return true;
            if(k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_DOWN){if(down)moveMenuFocus(k==KeyEvent.KEYCODE_DPAD_DOWN?1:-1);return true;}
            if(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER){if(!down)clickMenuFocus();return true;}
            if(k==KeyEvent.KEYCODE_MENU||k==4077){if(down&&e.getRepeatCount()==0)closePanel();return true;}return false;
        }
        if(navigationLevel>0){
            if(navigationLevel==1&&(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT)){if(down)options.focus(Math.max(0,Math.min(2,selectedSetting+(k==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1))));return true;}
            if(navigationLevel==1&&(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER)){if(!down&&e.getRepeatCount()==0)openSetting(selectedSetting);return true;}
            if(k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_BACK){if(!down){if(navigationLevel==2)selectSettings();else enterPlayback();}return true;}
            if(k==KeyEvent.KEYCODE_DPAD_DOWN){if(down&&e.getRepeatCount()==0&&navigationLevel==1)enterEpisodes();return true;}
            if(k==KeyEvent.KEYCODE_MENU||k==4077){if(down&&e.getRepeatCount()==0)enterPlayback();return true;}return false;
        }
        if(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT){if(down)seek(k==KeyEvent.KEYCODE_DPAD_LEFT?-10:10);return true;}
        if(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER||k==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){if(!down){command("toggle",null);wake();}return true;}
        if(k==KeyEvent.KEYCODE_MEDIA_PLAY||k==KeyEvent.KEYCODE_MEDIA_PAUSE){if(!down&&state.optBoolean("paused")== (k==KeyEvent.KEYCODE_MEDIA_PLAY))command("toggle",null);wake();return true;}
        if(k==KeyEvent.KEYCODE_DPAD_DOWN||k==KeyEvent.KEYCODE_MENU||k==4077){if(down&&e.getRepeatCount()==0)selectSettings();return true;}
        if(k==KeyEvent.KEYCODE_DPAD_UP){if(down)wake();return true;}
        if(k==KeyEvent.KEYCODE_BACK){if(!down){if(chrome){showChrome(false);handler.removeCallbacks(hide);}else closePlayer();}return true;}return false;
    }
    private String rate(double n){return String.format(Locale.US,"%s×",n==(int)n?String.valueOf((int)n):String.valueOf(n));}
    private static String format(double seconds){long s=Math.max(0,(long)seconds);return s>=3600?String.format(Locale.US,"%d:%02d:%02d",s/3600,s/60%60,s%60):String.format(Locale.US,"%d:%02d",s/60,s%60);}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size){TextView t=new TextView(activity);t.setText(value);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setShadowLayer(dp(2),0,dp(1),0xd9000000);return t;}
    private void updateText(TextView view,String value){if(!view.getText().toString().equals(value))view.setText(value);}
    private Button button(String label,Runnable action){Button b=new Button(activity);b.setText(label);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setStateListAnimator(null);b.setBackground(glass.surface(b,14,true));b.setOnClickListener(v->action.run());return b;}
    private ImageButton icon(int res,String label,Runnable action){ImageButton b=new ImageButton(activity);b.setImageResource(res);b.setImageTintList(ColorStateList.valueOf(Color.WHITE));b.setContentDescription(label);b.setPadding(dp(8),dp(8),dp(8),dp(8));b.setBackgroundColor(Color.TRANSPARENT);b.setFocusable(false);b.setOnClickListener(v->action.run());return b;}
    private void toast(String text){Toast.makeText(activity,text,Toast.LENGTH_SHORT).show();}
}
