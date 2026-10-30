package com.gathin.tvhome;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.*;
import android.graphics.Color;
import android.net.*;
import android.net.wifi.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Liquid Control Center and Wi-Fi sheet in a transient task. */
public final class QuickSettingsActivity extends Activity {
    static final String PAGE="liquid_page";
    private LinearLayout panel, body;
    private TextView title, subtitle;
    private String page="control";
    private View first;
    private boolean receiverRegistered;
    private final BroadcastReceiver networkReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){if("wifi".equals(page)) showWifi();}
    };
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LauncherLocale.wrap(base));}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout scrim=new FrameLayout(this);scrim.setBackgroundColor(0x16000000);scrim.setOnClickListener(v->finish());scrim.setFocusable(false);
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(16),dp(16),dp(16),dp(13));
        panel.setBackground(new LiquidGlass(panel,28,false));panel.setElevation(dp(12));panel.setOnClickListener(v->{});panel.setFocusable(false);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(350),ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.END);
        p.topMargin=dp(94);p.rightMargin=dp(56);scrim.addView(panel,p);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        title=text(getString(R.string.lg_control_center),19,Color.WHITE);title.setTypeface(null,android.graphics.Typeface.BOLD);
        heading.addView(title,new LinearLayout.LayoutParams(0,dp(29),1));
        subtitle=text(getString(R.string.lg_android_version, Build.VERSION.RELEASE),10,0xcce3ebff);heading.addView(subtitle);panel.addView(heading);
        ScrollView scroll=new ScrollView(this){
            @Override protected void onMeasure(int widthSpec,int heightSpec){
                int limit=getResources().getDisplayMetrics().heightPixels-dp(176);
                super.onMeasure(widthSpec,View.MeasureSpec.makeMeasureSpec(Math.max(dp(140),limit),View.MeasureSpec.AT_MOST));
            }
        };scroll.setVerticalScrollBarEnabled(false);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);panel.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        setContentView(scrim);
        if("wifi".equals(getIntent().getStringExtra(PAGE)))showWifi();else showControl();
        panel.setAlpha(0);panel.setTranslationY(-dp(8));panel.animate().alpha(1).translationY(0).setDuration(180).start();
    }
    @Override protected void onStart(){
        super.onStart();IntentFilter f=new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        f.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(networkReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(networkReceiver,f);
        receiverRegistered=true;
    }
    @Override protected void onStop(){if(receiverRegistered){unregisterReceiver(networkReceiver);receiverRegistered=false;}super.onStop();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);finish();}
    @Override protected void onPause(){super.onPause();finish();overridePendingTransition(0,0);}
    @Override public void onBackPressed(){if(!"control".equals(page)&&!"wifi".equals(getIntent().getStringExtra(PAGE)))showControl();else finish();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(e.getKeyCode()==4077||e.getKeyCode()==KeyEvent.KEYCODE_SETTINGS||e.getKeyCode()==83){if(e.getAction()==KeyEvent.ACTION_UP)finish();return true;}
        return super.dispatchKeyEvent(e);
    }
    private void reset(String name,String detail){page=detail;title.setText(name);subtitle.setText("control".equals(detail)?getString(R.string.lg_android_version, Build.VERSION.RELEASE):getString(R.string.lg_back));first=null;body.removeAllViews();}
    private void focusFirst(){if(first!=null){View target=first;target.post(target::requestFocus);}}
    private void showControl(){
        reset(getString(R.string.lg_control_center),"control");
        pair("wifi",getString(R.string.lg_network),networkName(),this::showWifi,"settings",getString(R.string.lg_tv_settings),getString(R.string.lg_android_settings),()->system(Settings.ACTION_SETTINGS));
        pair("picture",getString(R.string.lg_picture),getString(R.string.lg_display_brightness),()->system(Settings.ACTION_DISPLAY_SETTINGS),"sound",getString(R.string.lg_sound),getString(R.string.lg_audio_settings),()->system(Settings.ACTION_SOUND_SETTINGS));
        pair("appearance",getString(R.string.lg_appearance),getString(R.string.lg_launcher_settings),()->open(new Intent(this,MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS)),
                "clock",getString(R.string.lg_clock),getSharedPreferences("liquid_ui",0).getBoolean("clock24",true)?getString(R.string.lg_format24):getString(R.string.lg_format12),this::showClock);
        LinearLayout quick=new LinearLayout(this);quick.setGravity(Gravity.CENTER);
        action(quick,"moon",getString(R.string.lg_screensaver),()->open(new Intent(this,ClockActivity.class)));
        action(quick,"apps",getString(R.string.lg_apps),()->system(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));action(quick,"close",getString(R.string.lg_close),this::finish);
        LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,dp(51));q.topMargin=dp(5);body.addView(quick,q);
        TextView brightness=tile("sun",getString(R.string.lg_brightness),getString(R.string.lg_picture_settings),()->system(Settings.ACTION_DISPLAY_SETTINGS));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48));bp.topMargin=dp(5);body.addView(brightness,bp);
        ActivityManager am=getSystemService(ActivityManager.class);ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();am.getMemoryInfo(info);
        String memory=getString(R.string.lg_memory,(info.totalMem-info.availMem)/1073741824.0,info.totalMem/1073741824.0);
        TextView footer=text("Liquid Glass                 "+memory,10,0xbbe5edff);footer.setPadding(dp(2),dp(11),0,0);body.addView(footer);focusFirst();
    }
    private void showClock(){reset(getString(R.string.lg_clock),"clock");row("clock",getString(R.string.lg_format24),"14:30",()->clock(true));row("clock",getString(R.string.lg_format12),"2:30 PM",()->clock(false));row("moon",getString(R.string.lg_screensaver),getString(R.string.lg_screensaver_hint),()->open(new Intent(this,ClockActivity.class)));focusFirst();}
    private void clock(boolean value){getSharedPreferences("liquid_ui",0).edit().putBoolean("clock24",value).apply();showControl();}
    @SuppressWarnings("deprecation") private String networkName(){
        ConnectivityManager cm=getSystemService(ConnectivityManager.class);NetworkCapabilities caps=cm.getNetworkCapabilities(cm.getActiveNetwork());
        if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))return getString(R.string.lg_ethernet_connected);
        WifiManager wifi=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(wifi==null||!wifi.isWifiEnabled())return getString(R.string.lg_wifi_off);
        try{String ssid=wifi.getConnectionInfo().getSSID();if(ssid!=null&&!"<unknown ssid>".equals(ssid))return ssid.replace("\"","");}catch(SecurityException ignored){}
        return caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?getString(R.string.lg_wifi_connected):getString(R.string.lg_not_connected);
    }
    @SuppressWarnings("deprecation") private void showWifi(){
        View focused=getCurrentFocus();CharSequence label=focused==null?null:focused.getContentDescription();
        reset(getString(R.string.lg_wifi_network),"wifi");row("wifi",networkName(),getString(R.string.lg_current_connection),()->system(Settings.ACTION_WIFI_SETTINGS));
        row("settings",getString(R.string.lg_manage_networks),getString(R.string.lg_manage_networks_hint),()->system(Settings.ACTION_WIFI_SETTINGS));
        TextView caption=text(getString(R.string.lg_available_networks),10,0xbbe5edff);caption.setPadding(dp(4),dp(12),0,dp(5));body.addView(caption);
        WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);Map<String,ScanResult> networks=new TreeMap<>();
        if(wm!=null)try{for(ScanResult s:wm.getScanResults())if(s.SSID!=null&&!s.SSID.isEmpty()){ScanResult prev=networks.get(s.SSID);if(prev==null||s.level>prev.level)networks.put(s.SSID,s);}}catch(SecurityException ignored){}
        if(networks.isEmpty()){
            TextView hint=text(getString(R.string.lg_wifi_dialog_hint),12,0xdde5edff);hint.setSingleLine(false);hint.setPadding(dp(5),dp(8),dp(5),dp(12));body.addView(hint);
        }else{
            ArrayList<ScanResult> sorted=new ArrayList<>(networks.values());sorted.sort((a,b)->Integer.compare(b.level,a.level));
            for(ScanResult s:sorted.subList(0,Math.min(4,sorted.size()))){boolean secure=s.capabilities!=null&&(s.capabilities.contains("WPA")||s.capabilities.contains("WEP"));row("wifi",s.SSID,(secure?getString(R.string.lg_secured):getString(R.string.lg_open_network))+" · "+getString(R.string.lg_select_network),()->system(Settings.ACTION_WIFI_SETTINGS));}
        }
        TextView note=text(getString(R.string.lg_password_note),10,0xaadde5f5);note.setPadding(dp(4),dp(10),0,0);body.addView(note);
        if(label!=null)for(int n=0;n<body.getChildCount();n++){View v=body.getChildAt(n);if(label.equals(v.getContentDescription())){v.requestFocus();return;}}
        focusFirst();
    }
    private void pair(String ia,String a,String ah,Runnable ar,String ib,String b,String bh,Runnable br){
        LinearLayout row=new LinearLayout(this);TextView one=tile(ia,a,ah,ar),two=tile(ib,b,bh,br);
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(55),1);left.rightMargin=dp(6);row.addView(one,left);row.addView(two,new LinearLayout.LayoutParams(0,dp(55),1));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(7);body.addView(row,p);
    }
    private void row(String icon,String label,String hint,Runnable action){TextView t=tile(icon,label,hint,action);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(53));p.topMargin=dp(6);body.addView(t,p);}
    private TextView tile(String icon,String label,String hint,Runnable action){
        TextView t=text(label+"\n"+hint,13,Color.WHITE);android.text.SpannableString content=new android.text.SpannableString(label+"\n"+hint);
        if(!hint.isEmpty()){content.setSpan(new android.text.style.RelativeSizeSpan(.76f),label.length()+1,content.length(),0);content.setSpan(new android.text.style.ForegroundColorSpan(0xccdae5f6),label.length()+1,content.length(),0);}
        t.setText(content);t.setGravity(Gravity.CENTER_VERTICAL);t.setMaxLines(2);t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setPadding(dp(10),dp(4),dp(6),dp(4));t.setCompoundDrawablePadding(dp(8));android.graphics.drawable.Drawable glyph=MaterialIcons.get(this,icon,dp(22));t.setCompoundDrawables(glyph,null,null,null);
        t.setBackground(new LiquidGlass(t,18,true));t.setFocusable(true);t.setClickable(true);t.setId(View.generateViewId());t.setContentDescription(label);t.setOnClickListener(v->action.run());if(first==null)first=t;return t;
    }
    private void action(LinearLayout parent,String icon,String label,Runnable run){
        TextView v=tile(icon,"","",run);v.setText("");v.setContentDescription(label);v.setGravity(Gravity.CENTER);v.setPadding(dp(11),0,dp(11),0);v.setCompoundDrawablePadding(0);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(44),dp(44));p.leftMargin=dp(12);p.rightMargin=dp(12);parent.addView(v,p);
    }
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void system(String action){open(SystemSettings.intent(this,action));}
    private void open(Intent target){try{startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(ActivityNotFoundException|SecurityException e){Toast.makeText(this,R.string.not_available,Toast.LENGTH_SHORT).show();}}
}
