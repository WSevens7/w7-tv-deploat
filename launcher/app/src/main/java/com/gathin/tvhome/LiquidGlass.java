package com.gathin.tvhome;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Cached wallpaper material for Android 8–11: no RenderEffect and no idle render loop. */
final class LiquidGlass extends Drawable {
    private static final String OPACITY_KEY = "glass_opacity";
    private static int opacityPercent = 100;
    private static final String HERO_OPACITY_KEY = "hero_glass_opacity";
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private static final ArrayList<WeakReference<View>> OWNERS = new ArrayList<>();
    private static Bitmap backdrop, frost;
    private static int generation;
    private final View owner;
    private final float radius;
    private final boolean control;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF rect = new RectF();
    private final RectF layerBounds = new RectF();
    private final Matrix matrix = new Matrix();
    private final int[] location = new int[2];
    private Bitmap source;
    private BitmapShader texture;
    private Bitmap lensSource;
    private BitmapShader lens;
    private LinearGradient sheen, rim;
    private int alpha = 255;

    LiquidGlass(View owner, float radiusDp, boolean control) {
        this.owner = owner;
        this.radius = radiusDp * owner.getResources().getDisplayMetrics().density;
        this.control = control;
        opacityPercent = opacity(owner.getContext());
        OWNERS.add(new WeakReference<>(owner));
    }

    static int opacity(android.content.Context context) {
        return Math.max(0, Math.min(100, context.getSharedPreferences(Config.LIQUID_PREFS, 0).getInt(OPACITY_KEY, 100)));
    }

    static void setOpacity(android.content.Context context, int value) {
        opacityPercent = Math.max(0, Math.min(100, value));
        context.getSharedPreferences(Config.LIQUID_PREFS, 0).edit().putInt(OPACITY_KEY, opacityPercent).remove(HERO_OPACITY_KEY).apply();
        invalidateOwners();
    }

    static void resetOpacity(android.content.Context context){
        setOpacity(context,100);
    }
    private static void invalidateOwners(){
        OWNERS.removeIf(ref -> ref.get() == null);
        for (WeakReference<View> ref : OWNERS) {
            View view = ref.get();
            if (view != null) view.invalidate();
        }
    }

    static void clearWallpaper(){generation++;backdrop=null;frost=null;invalidateOwners();}

    static void wallpaper(Bitmap bitmap, View root) {
        if (bitmap == null || bitmap.isRecycled()) return;
        int token = ++generation;
        int w = 480, h = Math.max(1, Math.round(w * root.getResources().getDisplayMetrics().heightPixels
                / (float) root.getResources().getDisplayMetrics().widthPixels));
        Bitmap small = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(small);
        float scale = Math.max(w / (float) bitmap.getWidth(), h / (float) bitmap.getHeight());
        float rw = bitmap.getWidth() * scale, rh = bitmap.getHeight() * scale;
        // WallpaperPlayer keeps a hardware-only GPU bitmap; a software Canvas cannot sample it.
        Bitmap readable = bitmap.getConfig()==Bitmap.Config.HARDWARE ? bitmap.copy(Bitmap.Config.ARGB_8888,false) : bitmap;
        if(readable==null){small.recycle();return;}
        c.drawBitmap(readable, null, new RectF((w-rw)/2, (h-rh)/2, (w+rw)/2, (h+rh)/2),
                new Paint(Paint.FILTER_BITMAP_FLAG));
        if(readable!=bitmap)readable.recycle();
        WORK.execute(() -> {
            Bitmap blurred = blur(small);
            root.post(() -> {
                if (token != generation) { small.recycle(); blurred.recycle(); return; }
                // Old shaders can still be referenced by a queued render; let Android reclaim them safely.
                backdrop = small;
                frost = blurred;
                OWNERS.removeIf(ref -> ref.get() == null);
                for (WeakReference<View> ref : OWNERS) {
                    View v = ref.get();
                    if (v != null) v.invalidate();
                }
            });
        });
    }

    private static Bitmap blur(Bitmap b) {
        int w=b.getWidth(), h=b.getHeight();
        int[] a=new int[w*h], tmp=new int[w*h];
        b.getPixels(a,0,w,0,0,w,h);
        // Two box passes per axis at quarter resolution: stable, small, and recalculated only on wallpaper change.
        int r=5;
        for (int pass=0;pass<2;pass++) {
            for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
                int rr=0,gg=0,bb=0;
                for(int k=-r;k<=r;k++){int p=a[y*w+Math.max(0,Math.min(w-1,x+k))];rr+=(p>>16)&255;gg+=(p>>8)&255;bb+=p&255;}
                tmp[y*w+x]=0xff000000|((rr/11)<<16)|((gg/11)<<8)|(bb/11);
            }
            for (int y=0;y<h;y++) for(int x=0;x<w;x++) {
                int rr=0,gg=0,bb=0;
                for(int k=-r;k<=r;k++){int p=tmp[Math.max(0,Math.min(h-1,y+k))*w+x];rr+=(p>>16)&255;gg+=(p>>8)&255;bb+=p&255;}
                a[y*w+x]=0xff000000|((rr/11)<<16)|((gg/11)<<8)|(bb/11);
            }
        }
        return Bitmap.createBitmap(a,w,h,Bitmap.Config.ARGB_8888);
    }

    @Override protected void onBoundsChange(Rect bounds) {
        rect.set(bounds); rect.inset(1,1);
        sheen = new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,
                new int[]{0x40ffffff,0x0dffffff,0x06374b70,0x184d76b0},
                new float[]{0,.36f,.68f,1},Shader.TileMode.CLAMP);
        rim = new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,
                new int[]{0xe0ffffff,0x38ffffff,0x16ffffff,0x80c9deff},null,Shader.TileMode.CLAMP);
    }

    @Override public void draw(Canvas c) {
        if (rect.isEmpty()) return;
        float d=owner.getResources().getDisplayMetrics().density;
        float r=Math.min(radius,rect.height()/2);
        boolean focused=false;
        for(int state:getState()) if(state==android.R.attr.state_focused || state==android.R.attr.state_selected) focused=true;
        // Composite the material as one layer; labels and icons are outside this Drawable.
        int opacity=opacityPercent;
        layerBounds.set(rect);layerBounds.inset(-2*d,-2*d);
        int layer = opacity < 100 ? c.saveLayerAlpha(layerBounds, Math.round(opacity * 2.55f)) : -1;
        paint.setAlpha(alpha); paint.setStyle(Paint.Style.FILL); paint.setShader(null);
        if (!control && frost!=null) {
            if(source!=frost){source=frost;texture=new BitmapShader(source,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);}
            owner.getLocationOnScreen(location);
            float sw=owner.getResources().getDisplayMetrics().widthPixels;
            float sh=owner.getResources().getDisplayMetrics().heightPixels;
            matrix.setScale(sw/source.getWidth(),sh/source.getHeight());
            matrix.postTranslate(-location[0],-location[1]);
            texture.setLocalMatrix(matrix); paint.setShader(texture);
            c.drawRoundRect(rect,r,r,paint); paint.setShader(null);
        }
        paint.setColor(control ? (focused?0xc0314d75:0x19394362) : 0x8c192333);
        paint.setAlpha((paint.getColor()>>>24)*alpha/255);
        c.drawRoundRect(rect,r,r,paint);
        paint.setAlpha(alpha); paint.setShader(sheen);
        c.drawRoundRect(rect,r,r,paint); paint.setShader(null);
        // A thin magnified wallpaper rim simulates lens displacement while leaving text untouched.
        if (!control && backdrop!=null) {
            if(lensSource!=backdrop){lensSource=backdrop;lens=new BitmapShader(backdrop,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);}
            owner.getLocationOnScreen(location);
            float sw=owner.getResources().getDisplayMetrics().widthPixels;
            float sh=owner.getResources().getDisplayMetrics().heightPixels;
            matrix.setScale(sw/backdrop.getWidth()*1.035f,sh/backdrop.getHeight()*1.035f);
            matrix.postTranslate(-location[0]-sw*.0175f,-location[1]-sh*.0175f);
            lens.setLocalMatrix(matrix); paint.setShader(lens); paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.3f*d); paint.setAlpha(alpha*110/255);
            c.drawRoundRect(rect,r,r,paint);
        }
        paint.setStyle(Paint.Style.STROKE); paint.setShader(rim); paint.setAlpha(alpha);
        paint.setStrokeWidth((focused?1.5f:.65f)*d); c.drawRoundRect(rect,r,r,paint);
        paint.setShader(null); paint.setStyle(Paint.Style.FILL);
        if (layer != -1) c.restoreToCount(layer);
        // Keep keyboard/D-pad focus visible even with the material set fully transparent.
        if (control && focused && opacity < 70) {
            paint.setStyle(Paint.Style.STROKE); paint.setColor(0xE6FFFFFF); paint.setAlpha(alpha);
            paint.setStrokeWidth(1.5f*d); c.drawRoundRect(rect,r,r,paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    @Override public boolean isStateful(){return control;}
    @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
    @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
