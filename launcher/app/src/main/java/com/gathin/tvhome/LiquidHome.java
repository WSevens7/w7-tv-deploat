package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Composition helpers; all three shortcuts share ONE material surface. */
final class LiquidHome {
    static void refresh(Activity a) {
        boolean clock24=a.getSharedPreferences("liquid_ui",0).getBoolean("clock24",true);
        TextClock clock=a.findViewById(R.id.clock),ampm=a.findViewById(R.id.ampm);
        if(clock!=null){clock.setFormat12Hour(clock24?"HH:mm":"h:mm");clock.setFormat24Hour(clock24?"HH:mm":"h:mm");}
        if(ampm!=null){ampm.setFormat12Hour(clock24?"":"a");ampm.setFormat24Hour(clock24?"":"a");}
        if(ampm!=null)ampm.setVisibility(clock24?View.GONE:View.VISIBLE);
    }
    static void apply(Activity a) {
        LinearLayout top=a.findViewById(R.id.topButtons);
        float d=a.getResources().getDisplayMetrics().density;
        top.setBackground(new LiquidGlass(top,36,false));
        top.setPadding((int)(6*d),(int)(5*d),(int)(6*d),(int)(5*d));
        top.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams tlp=(RelativeLayout.LayoutParams)top.getLayoutParams();
        tlp.removeRule(RelativeLayout.ALIGN_BOTTOM);
        tlp.addRule(RelativeLayout.ALIGN_TOP,R.id.leftBlock);
        tlp.bottomMargin=0;
        top.setLayoutParams(tlp);
        int[] ids={R.id.btnInputs,R.id.btnWifi,R.id.btnSettings};
        int[] icons={R.drawable.ic_input,R.drawable.ic_wifi,R.drawable.ic_settings};
        String[] titles={a.getString(R.string.lg_tv_inputs),a.getString(R.string.lg_wifi_network),a.getString(R.string.lg_control_center)};
        for(int i=0;i<ids.length;i++) {
            TextView b=a.findViewById(ids[i]);
            b.setText(""); b.setContentDescription(titles[i]);
            b.setCompoundDrawablesRelativeWithIntrinsicBounds(icons[i],0,0,0);
            b.setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));
            b.setCompoundDrawablePadding(0);
            b.setPadding((int)(13*d),0,(int)(13*d),0);
            b.setGravity(Gravity.CENTER);
            b.setLayoutParams(new LinearLayout.LayoutParams((int)(48*d),(int)(44*d)));
            b.setBackground(focusBackground(d));
            b.setOnFocusChangeListener((v,on)->v.animate().scaleX(on?1.04f:1).scaleY(on?1.04f:1).setDuration(130).start());
        }
        a.findViewById(R.id.btnReorder).setVisibility(View.GONE);
        a.findViewById(R.id.btnSettings).setOnClickListener(v->a.startActivity(new Intent(a,QuickSettingsActivity.class)));
        View hero=a.findViewById(R.id.hero);
        hero.setPadding((int)(20*d),(int)(12*d),(int)(24*d),(int)(12*d));
        hero.setBackground(new LiquidGlass(hero,24,false));
        hero.forceHasOverlappingRendering(true);
        ((TextView)a.findViewById(R.id.heroName)).setTextSize(32);
        styleClockHeader(a,d);
        View weather=a.findViewById(R.id.weatherCardInclude);
        if(weather!=null)weather.setBackground(new LiquidGlass(weather,22,false));
    }

    /** Plain cinematic header: time | red accent | greeting and complete local date. */
    private static void styleClockHeader(Activity a,float d) {
        LinearLayout header=a.findViewById(R.id.leftBlock);
        TextClock clock=a.findViewById(R.id.clock),ampm=a.findViewById(R.id.ampm),date=a.findViewById(R.id.date);
        TextView greeting=a.findViewById(R.id.greeting);
        View divider=a.findViewById(R.id.clockDivider);
        LinearLayout details=(LinearLayout)date.getParent();
        // Keep the existing TextClocks and greeting reference so locale, time updates and 12/24h still work.
        for(View v:new View[]{clock,ampm,greeting,divider,details}) {
            if(v.getParent() instanceof ViewGroup)((ViewGroup)v.getParent()).removeView(v);
        }
        header.removeAllViews();
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Math.round(4*d),Math.round(4*d),0,Math.round(4*d));
        header.setBackground(null);
        header.setElevation(0);
        RelativeLayout.LayoutParams hp=(RelativeLayout.LayoutParams)header.getLayoutParams();
        hp.width=ViewGroup.LayoutParams.WRAP_CONTENT;
        hp.addRule(RelativeLayout.START_OF,R.id.topButtons);
        header.setLayoutParams(hp);

        LinearLayout time=new LinearLayout(a);
        time.setGravity(Gravity.CENTER_VERTICAL);
        time.setOrientation(LinearLayout.HORIZONTAL);
        clock.setTypeface(UiFont.get(a,R.string.font_light));
        clock.setTextSize(52);
        clock.setLetterSpacing(-.04f);
        time.addView(clock,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-2,-2);
        ap.leftMargin=Math.round(5*d);ap.gravity=Gravity.BOTTOM;ap.bottomMargin=Math.round(7*d);
        time.addView(ampm,ap);
        header.addView(time,new LinearLayout.LayoutParams(-2,-2));

        divider.setVisibility(View.VISIBLE);
        divider.setBackgroundColor(0xFFE50914);
        LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(Math.max(2,Math.round(1.5f*d)),Math.round(38*d));
        line.leftMargin=Math.round(16*d);line.rightMargin=Math.round(16*d);line.gravity=Gravity.CENTER_VERTICAL;
        header.addView(divider,line);

        greeting.setTypeface(UiFont.get(a,R.string.font_medium));
        greeting.setTextSize(12);
        greeting.setTextColor(0xE6FFFFFF);
        greeting.setAllCaps(false);
        greeting.setLetterSpacing(.22f);
        greeting.setTranslationX(0);
        details.setGravity(Gravity.CENTER_VERTICAL);
        details.setPadding(0,0,0,0);
        details.addView(greeting,0,new LinearLayout.LayoutParams(-1,-2));
        date.setFormat12Hour("EEEE, d MMMM yyyy");date.setFormat24Hour("EEEE, d MMMM yyyy");
        date.setTypeface(UiFont.get(a,R.string.font_regular));
        date.setTextSize(14);
        date.setAllCaps(false);
        date.setLetterSpacing(.14f);
        date.setSingleLine(true);
        date.setTextColor(0xE6FFFFFF);
        LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=Math.round(3*d);
        date.setLayoutParams(dp);
        header.addView(details,new LinearLayout.LayoutParams(-2,-2));
    }

    static Drawable focusBackground(float d) {
        StateListDrawable s=new StateListDrawable();
        GradientDrawable selected=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0x65ffffff,0x25ffffff});
        selected.setCornerRadius(28*d); selected.setStroke((int)Math.max(1,d),0xaaffffff);
        s.addState(new int[]{android.R.attr.state_focused},selected);
        s.addState(new int[]{},new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        return s;
    }
}
