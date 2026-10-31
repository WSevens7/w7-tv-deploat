package com.w7.idlixwebtv;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;

/** One compact segmented capsule. Only the glass lens moves; labels stay fixed. */
final class PlayerOptions extends FrameLayout {
    interface Listener {void selected(int index);void open(int index);}
    private final View lens;
    private final TextView[] tabs=new TextView[3];
    private final Listener listener;
    private int selected,segmentWidth;
    private final int inset;

    PlayerOptions(Activity activity,VideoGlass glass,Listener listener){
        super(activity);this.listener=listener;inset=dp(4);setPadding(inset,inset,inset,inset);
        setBackground(glass.surface(this,30,false));setClipChildren(false);setClipToPadding(false);
        lens=new View(activity);lens.setSelected(true);lens.setBackground(glass.surface(lens,26,true));
        addView(lens,new FrameLayout.LayoutParams(1,-1));
        LinearLayout labels=new LinearLayout(activity);labels.setBaselineAligned(false);
        String[] names={"Kualitas","Subtitle","Lainnya"};
        for(int i=0;i<tabs.length;i++){
            final int index=i;TextView tab=new TextView(activity);tabs[i]=tab;
            tab.setId(View.generateViewId());tab.setText(names[i]);tab.setTextColor(Color.WHITE);tab.setTextSize(16);
            tab.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));tab.setGravity(Gravity.CENTER);
            tab.setShadowLayer(dp(2),0,dp(1),0xc0000000);tab.setFocusable(true);tab.setClickable(true);
            tab.setOnFocusChangeListener((v,on)->{if(on){selected=index;listener.selected(index);move(true);}else post(()->lens.setActivated(hasFocus()));});
            tab.setOnClickListener(v->{selected=index;listener.selected(index);move(true);listener.open(index);});
            tab.setOnKeyListener((v,key,event)->{
                if(key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT)return false;
                if(event.getAction()==KeyEvent.ACTION_DOWN)tabs[Math.max(0,Math.min(2,index+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)))].requestFocus();
                return true;
            });
            labels.addView(tab,new LinearLayout.LayoutParams(0,-1,1));
        }
        addView(labels,new FrameLayout.LayoutParams(-1,-1));
    }
    void focus(int index){selected=Math.max(0,Math.min(2,index));tabs[selected].requestFocus();move(true);}
    void describe(String quality,String subtitle){tabs[0].setContentDescription("Kualitas: "+quality);tabs[1].setContentDescription("Subtitle: "+subtitle);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        segmentWidth=Math.max(1,(MeasureSpec.getSize(widthSpec)-2*inset)/3);
        // Set width before FrameLayout measures children, so the first frame has a real lens.
        ((FrameLayout.LayoutParams)lens.getLayoutParams()).width=segmentWidth;
        super.onMeasure(widthSpec,heightSpec);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);move(false);}
    private void move(boolean animate){
        lens.setActivated(hasFocus());lens.animate().cancel();
        if(animate&&isLaidOut())lens.animate().translationX(selected*segmentWidth).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
        else lens.setTranslationX(selected*segmentWidth);
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
