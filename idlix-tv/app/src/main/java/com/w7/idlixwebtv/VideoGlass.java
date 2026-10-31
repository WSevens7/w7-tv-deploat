package com.w7.idlixwebtv;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.util.Base64;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.*;

/** Cached, reduced-resolution video glass approximation for Android 8–11. */
final class VideoGlass {
    private static final class Frame { final Bitmap clear,blur;Frame(Bitmap a,Bitmap b){clear=a;blur=b;} }
    private final Activity activity;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ArrayList<WeakReference<View>> owners=new ArrayList<>();
    private Frame frame;
    private static final Uri OPACITY_URI=Uri.parse("content://com.gathin.tvhome.appearance/opacity");
    private int opacityPercent;
    private boolean observing;
    private final ContentObserver appearanceObserver=new ContentObserver(handler){
        @Override public void onChange(boolean selfChange){readOpacity();}
    };
    private boolean busy,closed;
    private int frameCount;
    VideoGlass(Activity a){activity=a;opacityPercent=Math.max(0,Math.min(100,a.getSharedPreferences("appearance",0).getInt("last_launcher_opacity",100)));}
    void start(){
        if(!observing)try{activity.getContentResolver().registerContentObserver(OPACITY_URI,false,appearanceObserver);observing=true;}catch(SecurityException|IllegalArgumentException ignored){}
        readOpacity();
    }
    void stop(){if(observing){activity.getContentResolver().unregisterContentObserver(appearanceObserver);observing=false;}}
    private void readOpacity(){
        try(Cursor cursor=activity.getContentResolver().query(OPACITY_URI,new String[]{"opacity"},null,null,null)){
            if(cursor==null||!cursor.moveToFirst())return;
            int value=Math.max(0,Math.min(100,cursor.getInt(cursor.getColumnIndexOrThrow("opacity"))));
            if((activity.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0)android.util.Log.d("TVGlass","Launcher opacity: "+value+"%");
            if(value==opacityPercent)return;
            opacityPercent=value;
            activity.getSharedPreferences("appearance",0).edit().putInt("last_launcher_opacity",value).apply();
            owners.removeIf(r->r.get()==null);
            for(WeakReference<View> ref:owners){View view=ref.get();if(view!=null)view.invalidate();}
        }catch(SecurityException|IllegalArgumentException|IllegalStateException ignored){/* Keep the last known value if the launcher is unavailable. */}
    }
    Drawable surface(View owner,int radius,boolean control){owners.add(new WeakReference<>(owner));return new Surface(owner,radius,control);}
    boolean hasFrame(){return frame!=null;}
    int frameCount(){return frameCount;}
    void offer(String encoded){
        if(closed||busy||encoded==null||encoded.length()>120000||!encoded.startsWith("data:image/jpeg;base64,"))return;
        busy=true;
        worker.execute(()->{
            Frame next=null;
            try{byte[] data=Base64.decode(encoded.substring(encoded.indexOf(',')+1),Base64.DEFAULT);Bitmap b=BitmapFactory.decodeByteArray(data,0,data.length);if(b!=null&&b.getWidth()<=320&&b.getHeight()<=240)next=new Frame(b,blur(b));}
            catch(Exception|OutOfMemoryError ignored){}
            Frame result=next;handler.post(()->{busy=false;if(closed||result==null)return;frame=result;frameCount++;if(frameCount<=3&&(activity.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0)android.util.Log.d("TVGlass","Cached video glass frame "+frameCount+" · "+result.clear.getWidth()+"x"+result.clear.getHeight());owners.removeIf(r->r.get()==null);for(WeakReference<View> r:owners){View v=r.get();if(v!=null&&v.isShown())v.invalidate();}});
        });
    }
    void destroy(){stop();closed=true;worker.shutdownNow();handler.removeCallbacksAndMessages(null);owners.clear();frame=null;}
    private static Bitmap blur(Bitmap b){
        int w=b.getWidth(),h=b.getHeight(),radius=4;int[] a=new int[w*h],tmp=new int[w*h];b.getPixels(a,0,w,0,0,w,h);
        for(int pass=0;pass<2;pass++){
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){int r=0,g=0,blue=0;for(int k=-radius;k<=radius;k++){int c=a[y*w+Math.max(0,Math.min(w-1,x+k))];r+=c>>16&255;g+=c>>8&255;blue+=c&255;}tmp[y*w+x]=0xff000000|r/9<<16|g/9<<8|blue/9;}
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){int r=0,g=0,blue=0;for(int k=-radius;k<=radius;k++){int c=tmp[Math.max(0,Math.min(h-1,y+k))*w+x];r+=c>>16&255;g+=c>>8&255;blue+=c&255;}a[y*w+x]=0xff000000|r/9<<16|g/9<<8|blue/9;}
        }
        return Bitmap.createBitmap(a,w,h,Bitmap.Config.ARGB_8888);
    }
    private final class Surface extends Drawable {
        private final View owner;
        private final float radius,density;
        private final boolean control;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final RectF rect=new RectF(),inner=new RectF(),layerBounds=new RectF();
        private final Path rim=new Path();
        private final Matrix matrix=new Matrix(),lensMatrix=new Matrix();
        private final int[] position=new int[2];
        private Frame seen;
        private BitmapShader texture,lens;
        private Shader sheen,edge;
        private boolean lastFocus;
        private int alpha=255;
        Surface(View owner,int r,boolean control){this.owner=owner;this.control=control;density=owner.getResources().getDisplayMetrics().density;radius=r*density;}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
        @Override protected void onBoundsChange(Rect bounds){sheen=null;edge=null;}
        @Override public void getOutline(Outline outline){outline.setRoundRect(getBounds(),radius);}
        @Override public void draw(Canvas c){
            rect.set(getBounds());rect.inset(density,density);if(rect.isEmpty())return;
            boolean focus=owner.isFocused()||owner.isPressed()||owner.isActivated(),selected=owner.isSelected();
            if(sheen==null||lastFocus!=focus){lastFocus=focus;sheen=new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{focus?0x65ffffff:0x35ffffff,0x04ffffff,0x207ab4dc},new float[]{0,.48f,1},Shader.TileMode.CLAMP);edge=new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{focus?0xffffffff:0xbbe6f4ff,0x33657386,focus?0xffbcecff:0x778daabb},new float[]{0,.55f,1},Shader.TileMode.CLAMP);}
            // Only composite the material; Android draws text/icons above this Drawable.
            int materialAlpha=Math.round(opacityPercent*2.55f)*alpha/255;
            if(materialAlpha==0){if(focus)drawEdge(c,true);return;}
            layerBounds.set(rect);layerBounds.inset(-2*density,-2*density);
            int layer=materialAlpha<255?c.saveLayerAlpha(layerBounds,materialAlpha):-1;
            paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);paint.setShader(null);
            Frame f=frame;
            if(f!=null){
                if(seen!=f){seen=f;texture=new BitmapShader(f.blur,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);lens=new BitmapShader(f.clear,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);}
                View window=activity.getWindow().getDecorView();float w=window.getWidth(),h=window.getHeight();owner.getLocationInWindow(position);
                matrix.setScale(w/f.blur.getWidth(),h/f.blur.getHeight());matrix.postTranslate(-position[0],-position[1]);texture.setLocalMatrix(matrix);
                paint.setShader(texture);paint.setAlpha(control?180:210);c.drawRoundRect(rect,radius,radius,paint);
                // A thin offset clear rim suggests a lens; this is not a physical refraction shader.
                lensMatrix.setScale(w/f.clear.getWidth()*1.016f,h/f.clear.getHeight()*1.016f);lensMatrix.postTranslate(-position[0]-rect.centerX()*.016f,-position[1]-rect.centerY()*.016f);lens.setLocalMatrix(lensMatrix);
                inner.set(rect);inner.inset(3*density,3*density);rim.reset();rim.setFillType(Path.FillType.EVEN_ODD);rim.addRoundRect(rect,radius,radius,Path.Direction.CW);rim.addRoundRect(inner,Math.max(0,radius-3*density),Math.max(0,radius-3*density),Path.Direction.CW);
                paint.setShader(lens);paint.setAlpha(100);c.drawPath(rim,paint);
            }
            paint.setShader(null);paint.setAlpha(alpha);paint.setColor(focus?0x99304962:selected?0x99405465:control?0xa119222d:0xb019202a);c.drawRoundRect(rect,radius,radius,paint);
            paint.setShader(sheen);c.drawRoundRect(rect,radius,radius,paint);
            if(!focus)drawEdge(c,false);
            if(layer!=-1)c.restoreToCount(layer);
            // Focus must remain visible even at zero material opacity.
            if(focus)drawEdge(c,true);
        }
        private void drawEdge(Canvas c,boolean focus){paint.setAlpha(255);paint.setShader(edge);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth((focus?2f:1f)*density);c.drawRoundRect(rect,radius,radius,paint);paint.setShader(null);paint.setStyle(Paint.Style.FILL);}
        @Override public void setAlpha(int a){alpha=a;invalidateSelf();}
        @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
