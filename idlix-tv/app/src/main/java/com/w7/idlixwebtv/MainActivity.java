package com.w7.idlixwebtv;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;

/** Remote-operated browser shell. Playback and access remain controlled by the website. */
public final class MainActivity extends Activity {
    private static final String HOME="https://z2.idlixku.com/";
    private FrameLayout root, full;
    private LinearLayout toolbar;
    private WebView web;
    private TvPlayer player;
    private Cursor cursor;
    private TextView status;
    private Button cursorButton;
    private ProgressBar progress;
    private View video;
    private WebChromeClient.CustomViewCallback videoCallback;
    private boolean pointer=true, failed;
    private String pageTitle="";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable hideStatus=()->{if(status!=null)status.setVisibility(View.GONE);};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        immersive();
        root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        web=new WebView(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
        s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);
        // Playback is also requested by the native remote controls, outside WebView touch events.
        s.setMediaPlaybackRequiresUserGesture(false);s.setAllowFileAccess(false);s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){
                if(!r.isForMainFrame())return false;
                Uri u=r.getUrl();String host=u.getHost();
                if(!"https".equalsIgnoreCase(u.getScheme())){message("Tautan ini tidak didukung.");return true;}
                if(host!=null&&(host.equals("idlixku.com")||host.endsWith(".idlixku.com")))return false;
                // Top-level redirects to other sites require a real user navigation and confirmation.
                if(r.hasGesture())new AlertDialog.Builder(MainActivity.this).setTitle("Buka situs lain?")
                    .setMessage(host).setPositiveButton("Buka",(d,b)->web.loadUrl(u.toString()))
                    .setNegativeButton("Batal",null).show();
                else message("Pengalihan ke situs lain diblokir.");
                return true;
            }
            @Override public void onPageStarted(WebView w,String u,Bitmap icon){failed=false;progress.setVisibility(View.VISIBLE);}
            @Override public void onPageFinished(WebView w,String u){
                progress.setVisibility(View.GONE);CookieManager.getInstance().flush();
                if(!failed)message("Panah: kursor · OK: klik · Menu: kontrol · Back: kembali");
            }
            @Override public void onReceivedError(WebView w,WebResourceRequest r,WebResourceError e){
                if(r.isForMainFrame()){failed=true;message("Halaman gagal dimuat. Tekan Menu untuk muat ulang.");}
            }
            @Override public void onReceivedSslError(WebView w,SslErrorHandler h,android.net.http.SslError e){
                h.cancel();message("Sertifikat situs bermasalah. Koneksi dibatalkan.");
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView v,int p){progress.setProgress(p);}
            @Override public void onReceivedTitle(WebView v,String title){pageTitle=title==null?"":title;}
            @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean gesture,Message result){
                message("Jendela popup diblokir.");return false;
            }
            @Override public void onShowCustomView(View v,CustomViewCallback callback){
                if(video!=null){callback.onCustomViewHidden();return;}
                video=v;videoCallback=callback;web.setVisibility(View.INVISIBLE);
                toolbar.setVisibility(View.GONE);cursor.setVisibility(View.GONE);
                full=new FrameLayout(MainActivity.this);full.setBackgroundColor(Color.BLACK);
                full.addView(v,new FrameLayout.LayoutParams(-1,-1));root.addView(full,new FrameLayout.LayoutParams(-1,-1));if(player!=null&&player.isActive())player.bringToFront();immersive();
            }
            @Override public void onHideCustomView(){exitVideo();}
        });
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        root.addView(progress,new FrameLayout.LayoutParams(-1,dp(3),Gravity.TOP));
        cursor=new Cursor();root.addView(cursor,new FrameLayout.LayoutParams(-1,-1));
        toolbar=new LinearLayout(this);toolbar.setPadding(dp(16),dp(8),dp(16),dp(8));toolbar.setBackgroundColor(0xf0151b24);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        addButton("Beranda",()->{exitVideo();web.loadUrl(HOME);hideToolbar();});
        addButton("Muat ulang",()->{web.reload();hideToolbar();});
        cursorButton=addButton("Kursor: aktif",()->{pointer=!pointer;cursorButton.setText(pointer?"Kursor: aktif":"Kursor: mati");hideToolbar();});
        addButton("Player TV",()->{player.enable();hideToolbar();});
        addButton("Info",()->new AlertDialog.Builder(this).setTitle("IDLIX Web TV")
            .setMessage("Saat menjelajah: panah menggerakkan kursor, OK mengeklik. Gerakkan ke tepi atas/bawah untuk menggulir.\n\nSaat menonton: OK putar/jeda, kiri/kanan lompat 10 detik. Bawah pertama masuk pilihan pemutaran; kiri/kanan memilih dan OK membuka. Bawah lagi membuka episode dengan fokus pada episode yang sedang diputar. Atas/Back kembali satu tingkat.\n\n"+pageTitle+"\n"+web.getUrl()+"\n\nAplikasi pribadi; bukan aplikasi resmi IDLIX. Video, pilihan track, dan episode tetap berasal dari situs.")
            .setPositiveButton("Tutup",null).show());
        addButton("Tutup menu",this::hideToolbar);
        root.addView(toolbar,new FrameLayout.LayoutParams(-1,dp(64),Gravity.TOP));toolbar.setVisibility(View.GONE);
        status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(14);status.setPadding(dp(14),dp(8),dp(14),dp(8));status.setBackgroundColor(0xda151b24);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);sp.bottomMargin=dp(18);root.addView(status,sp);
        pointer=getPreferences(0).getBoolean("pointer",true);
        cursorButton.setText(pointer?"Kursor: aktif":"Kursor: mati");cursor.setVisibility(pointer?View.VISIBLE:View.GONE);
        player=new TvPlayer(this,web,root,on->{
            if(on){toolbar.setVisibility(View.GONE);cursor.setVisibility(View.GONE);status.setVisibility(View.GONE);}
            else if(video==null)cursor.setVisibility(pointer?View.VISIBLE:View.GONE);
        });
        if(state==null||web.restoreState(state)==null)web.loadUrl(HOME);
        web.requestFocus();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894);}
    private Button addButton(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);toolbar.addView(b,new LinearLayout.LayoutParams(0,-1,1));b.setOnClickListener(v->action.run());return b;}
    private void message(String text){status.setText(text);status.setVisibility(View.VISIBLE);handler.removeCallbacks(hideStatus);handler.postDelayed(hideStatus,6500);}
    private void hideToolbar(){toolbar.setVisibility(View.GONE);cursor.setVisibility(pointer&&video==null&&(player==null||!player.isActive())?View.VISIBLE:View.GONE);web.requestFocus();getPreferences(0).edit().putBoolean("pointer",pointer).apply();}
    private void exitVideo(){if(video==null)return;full.removeAllViews();root.removeView(full);video=null;web.setVisibility(View.VISIBLE);WebChromeClient.CustomViewCallback c=videoCallback;videoCallback=null;if(c!=null)c.onCustomViewHidden();hideToolbar();immersive();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(player!=null&&player.isActive())return player.handle(e)||super.dispatchKeyEvent(e);
        int k=e.getKeyCode();
        if(k==KeyEvent.KEYCODE_MENU||k==4077){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){if(toolbar.getVisibility()==View.VISIBLE)hideToolbar();else if(video==null){toolbar.setVisibility(View.VISIBLE);toolbar.getChildAt(0).requestFocus();cursor.setVisibility(View.GONE);}}return true;}
        if(k==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_UP)back();return true;}
        if(toolbar.getVisibility()==View.VISIBLE||video!=null)return super.dispatchKeyEvent(e);
        if(pointer){
            if(k>=KeyEvent.KEYCODE_DPAD_UP&&k<=KeyEvent.KEYCODE_DPAD_RIGHT){if(e.getAction()==KeyEvent.ACTION_DOWN)cursor.move(k,e.getRepeatCount());return true;}
            if(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER){if(e.getAction()==KeyEvent.ACTION_UP)cursor.click();return true;}
        }
        if(k==KeyEvent.KEYCODE_PAGE_DOWN||k==KeyEvent.KEYCODE_PAGE_UP){if(e.getAction()==KeyEvent.ACTION_DOWN)web.scrollBy(0,(k==KeyEvent.KEYCODE_PAGE_DOWN?1:-1)*web.getHeight()*3/4);return true;}
        return super.dispatchKeyEvent(e);
    }
    private void back(){if(video!=null){exitVideo();return;}if(toolbar.getVisibility()==View.VISIBLE){hideToolbar();return;}if(web.canGoBack()){web.goBack();return;}new AlertDialog.Builder(this).setMessage("Keluar dari IDLIX Web TV?").setPositiveButton("Keluar",(d,w)->finish()).setNegativeButton("Batal",null).show();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);web.saveState(b);}
    @Override protected void onPause(){super.onPause();if(player!=null)player.stop();web.onPause();CookieManager.getInstance().flush();}
    @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();if(player!=null)player.start();immersive();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(player!=null)player.destroy();exitVideo();root.removeView(web);web.destroy();super.onDestroy();}
    private final class Cursor extends View {
        private final Paint paint=new Paint(3);private float x=-1,y=-1;
        Cursor(){super(MainActivity.this);setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){x=w/2f;y=h/2f;}
        @Override protected void onDraw(Canvas c){if(x<0)return;paint.setStyle(Paint.Style.FILL);paint.setColor(0xaa000000);c.drawCircle(x,y,dp(9),paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(Color.WHITE);c.drawCircle(x,y,dp(8),paint);paint.setStyle(Paint.Style.FILL);c.drawCircle(x,y,dp(2),paint);}
        void move(int key,int repeat){float step=dp(repeat>5?32:18);if(key==19)y-=step;if(key==20)y+=step;if(key==21)x-=step;if(key==22)x+=step;
            if(y<dp(22)){web.scrollBy(0,-dp(70));y=dp(22);}if(y>getHeight()-dp(22)){web.scrollBy(0,dp(70));y=getHeight()-dp(22);}
            x=Math.max(dp(10),Math.min(getWidth()-dp(10),x));invalidate();}
        void click(){long t=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x,y,0);MotionEvent up=MotionEvent.obtain(t,t+60,MotionEvent.ACTION_UP,x,y,0);web.dispatchTouchEvent(down);web.dispatchTouchEvent(up);down.recycle();up.recycle();}
    }
}
