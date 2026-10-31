package com.w7.idlixwebtv;

import android.app.Activity;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** A horizontal TV episode rail. Current playback and keyboard focus are distinct states. */
final class EpisodeRail extends LinearLayout {
    interface Listener { void select(JSONObject episode); }
    private final Activity activity;
    private final Listener listener;
    private final VideoGlass glass;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService loader=Executors.newFixedThreadPool(2);
    private final LruCache<String,Bitmap> images=new LruCache<String,Bitmap>(2*1024*1024){
        @Override protected int sizeOf(String key,Bitmap value){return value.getByteCount();}
    };
    private final TextView heading;
    private final TextView navigationHint;
    private final HorizontalScrollView scroll;
    private final LinearLayout cards;
    private int generation;
    private boolean closed;

    EpisodeRail(Activity a,VideoGlass glass,Listener listener){
        super(a);activity=a;this.listener=listener;this.glass=glass;setOrientation(VERTICAL);setPadding(dp(24),dp(20),dp(24),dp(18));
        setBackground(glass.surface(this,28,false));
        heading=text("Episode",22);heading.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);addView(heading);
        navigationHint=text("",12);navigationHint.setTextColor(0xffbdbdbd);navigationHint.setPadding(0,dp(6),0,dp(14));addView(navigationHint);
        scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);scroll.setFocusable(false);
        cards=new LinearLayout(a);cards.setOrientation(HORIZONTAL);cards.setPadding(dp(3),dp(3),dp(3),dp(3));scroll.addView(cards);addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setVisibility(GONE);
    }
    void open(JSONArray episodes,int season){
        render(episodes,season,"",false,"");
    }
    void openMovies(JSONArray movies,String genre,String status){render(movies,0,genre,true,status);}
    private void render(JSONArray episodes,int season,String genre,boolean movies,String status){
        int token=++generation;cards.removeAllViews();heading.setText(movies?"Rekomendasi · "+(genre.isEmpty()?"Genre sama":genre)+" · Maks. 8 film":"Episode · Musim "+season+" · "+episodes.length()+" tersedia");
        navigationHint.setText(movies?"← / → Pilih film     OK Putar     ↑ / Back Kembali ke pilihan":"← / → Pilih episode     OK Putar     ↑ / Back Kembali ke pilihan");
        if(episodes.length()==0){TextView empty=text(status.equals("loading")?"Memuat rekomendasi dari sumber…":"Rekomendasi belum tersedia dari sumber.",18);empty.setPadding(dp(22),dp(20),dp(22),dp(20));empty.setBackground(glass.surface(empty,16,true));empty.setFocusable(true);empty.setId(View.generateViewId());empty.setNextFocusLeftId(empty.getId());empty.setNextFocusRightId(empty.getId());cards.addView(empty,new LinearLayout.LayoutParams(dp(610),dp(100)));setVisibility(VISIBLE);empty.post(empty::requestFocus);return;}
        ArrayList<View> items=new ArrayList<>();int current=0;
        for(int i=0;i<episodes.length();i++){
            JSONObject e=episodes.optJSONObject(i);if(e==null)continue;
            boolean playing=e.optBoolean("current");int number=e.optInt("number");if(playing)current=items.size();
            LinearLayout card=new LinearLayout(activity);card.setOrientation(VERTICAL);card.setId(View.generateViewId());card.setFocusable(true);card.setClickable(true);card.setSelected(playing);card.setPadding(dp(4),dp(4),dp(4),dp(4));
            card.setBackground(glass.surface(card,13,true));card.setClipToOutline(true);card.setContentDescription(movies?e.optString("title"):"Episode "+number+(playing?", sedang diputar":""));
            FrameLayout picture=new FrameLayout(activity);picture.setBackgroundColor(0xff202020);ImageView image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.CENTER_CROP);picture.addView(image,new FrameLayout.LayoutParams(-1,-1));
            TextView numberLabel=text(movies?e.optString("year"):String.format(Locale.US,"S%02d E%02d",season,number),12);numberLabel.setPadding(dp(7),dp(3),dp(7),dp(3));numberLabel.setBackgroundColor(0xbb18202a);
            FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);np.leftMargin=dp(5);np.topMargin=dp(5);picture.addView(numberLabel,np);
            if(playing){TextView badge=text("DIPUTAR",9);badge.setPadding(dp(5),dp(4),dp(5),dp(4));badge.setBackgroundColor(0xff426b86);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);picture.addView(badge,bp);}
            card.addView(picture,new LinearLayout.LayoutParams(-1,dp(92)));
            TextView label=text(movies?e.optString("title"):"Episode "+number,16);label.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);label.setPadding(dp(6),dp(6),dp(6),0);label.setMaxLines(movies?2:1);label.setEllipsize(android.text.TextUtils.TruncateAt.END);if(movies)label.setMinLines(2);card.addView(label);
            String name=movies?genre:e.optString("title");if(!movies&&name.equalsIgnoreCase("Episode "+number))name=playing?"Sedang diputar":"Musim "+season;
            TextView detail=text(name,11);detail.setTextColor(0xffbbbbbb);detail.setPadding(dp(6),dp(3),dp(6),dp(4));detail.setSingleLine(true);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(detail);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(176),dp(movies?173:151));cp.rightMargin=dp(13);cards.addView(card,cp);items.add(card);
            card.setOnClickListener(v->listener.select(e));card.setOnFocusChangeListener((v,on)->{if(on)center(v);});
            load(image,e.optString("thumbnail"),token);
        }
        for(int i=0;i<items.size();i++){View v=items.get(i);v.setNextFocusLeftId(items.get(Math.max(0,i-1)).getId());v.setNextFocusRightId(items.get(Math.min(items.size()-1,i+1)).getId());v.setNextFocusDownId(v.getId());}
        setVisibility(VISIBLE);if(!items.isEmpty()){View target=items.get(Math.min(current,items.size()-1));scroll.post(()->{target.requestFocus();scroll.scrollTo(Math.max(0,target.getLeft()-(scroll.getWidth()-target.getWidth())/2),0);});}
    }
    private void center(View v){scroll.post(()->scroll.smoothScrollTo(Math.max(0,v.getLeft()-(scroll.getWidth()-v.getWidth())/2),0));}
    private void load(ImageView view,String source,int token){
        if(source.isEmpty())return;view.setTag(source);Bitmap cached=images.get(source);if(cached!=null){view.setImageBitmap(cached);return;}
        loader.execute(()->{Bitmap bitmap=download(source);if(bitmap==null)return;handler.post(()->{if(closed)return;images.put(source,bitmap);if(token==generation&&source.equals(view.getTag()))view.setImageBitmap(bitmap);});});
    }
    private Bitmap download(String source){
        HttpURLConnection connection=null;
        try{
            URL url=new URL(source);String host=url.getHost().toLowerCase(Locale.US);
            if(!url.getProtocol().equals("https")||!(host.equals("image.tmdb.org")||host.equals("idlixku.com")||host.endsWith(".idlixku.com")))return null;
            connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(4000);connection.setReadTimeout(4000);connection.setInstanceFollowRedirects(false);
            if(connection.getResponseCode()!=200||connection.getContentLengthLong()>2*1024*1024)return null;
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))>0){if(bytes.size()+n>2*1024*1024)return null;bytes.write(chunk,0,n);}}
            byte[] data=bytes.toByteArray();BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);
            if(o.outWidth<=0||o.outHeight<=0)return null;o.inSampleSize=1;while(o.outWidth/o.inSampleSize>480||o.outHeight/o.inSampleSize>270)o.inSampleSize*=2;o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeByteArray(data,0,data.length,o);
        }catch(Exception|OutOfMemoryError failure){return null;}finally{if(connection!=null)connection.disconnect();}
    }
    void destroy(){closed=true;generation++;loader.shutdownNow();handler.removeCallbacksAndMessages(null);images.evictAll();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size){TextView v=new TextView(activity);v.setText(s);v.setTextSize(size);v.setTextColor(Color.WHITE);return v;}
}
