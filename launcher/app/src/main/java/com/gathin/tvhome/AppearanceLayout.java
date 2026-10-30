package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

/** TV settings shell: category navigation on the left, one scrollable page on the right. */
final class AppearanceLayout {
    private final Activity activity;
    private final ViewFlipper pages;
    private final List<TextView> navigation=new ArrayList<>();
    private int selected;

    static AppearanceLayout install(Activity a){return new AppearanceLayout(a);}
    private AppearanceLayout(Activity a){
        activity=a;
        LinearLayout old=(LinearLayout)((ScrollView)((ViewGroup)a.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(0);
        View columns=(View)a.findViewById(R.id.columns4).getParent();View columnsLabel=before(columns);
        View rows=(View)a.findViewById(R.id.rows0).getParent();View rowsLabel=before(rows);
        View presets=(View)a.findViewById(R.id.dock100).getParent();View opacityLabel=before(presets);
        View advancedHelp=after(a.findViewById(R.id.advancedChoice));
        View themeHelp=after(a.findViewById(R.id.followThemeChoice));
        View homeHelp=after(a.findViewById(R.id.defaultHomeChoice));
        List<View> wallpaper=Arrays.asList(a.findViewById(R.id.wallpaperChoice),a.findViewById(R.id.wallpaperAutoSwitch),a.findViewById(R.id.wallpaperIntervalChoice));
        List<View> glass=Arrays.asList(opacityLabel,a.findViewById(R.id.glassOpacityPreview),a.findViewById(R.id.glassOpacitySlider),a.findViewById(R.id.glassOpacityHelp),presets);
        presets.setVisibility(View.GONE);
        List<View> dock=Arrays.asList(a.findViewById(R.id.reorderChoice),columnsLabel,columns,rowsLabel,rows);
        List<View> motion=Arrays.asList(a.findViewById(R.id.animationsChoice),a.findViewById(R.id.advancedChoice),advancedHelp);
        List<View> system=Arrays.asList(a.findViewById(R.id.openOnStartChoice),a.findViewById(R.id.followThemeChoice),themeHelp,
                a.findViewById(R.id.defaultHomeChoice),homeHelp,a.findViewById(R.id.tvSettingsChoice));
        LinearLayout shell=new LinearLayout(a);shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setPadding(dp(20),dp(20),dp(20),dp(20));shell.setBackgroundColor(0xff091219);
        LinearLayout sidebar=new LinearLayout(a);sidebar.setOrientation(LinearLayout.VERTICAL);sidebar.setPadding(dp(12),dp(14),dp(12),dp(12));
        sidebar.setBackground(panel());shell.addView(sidebar,new LinearLayout.LayoutParams(dp(216),-1));
        TextView heading=label(a.getString(R.string.lg_appearance),25);heading.setPadding(dp(8),0,0,dp(18));sidebar.addView(heading);
        pages=new ViewFlipper(a);pages.setBackground(panel());
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-1,1);pp.leftMargin=dp(16);shell.addView(pages,pp);
        int[] titles={R.string.lg_wallpaper,R.string.lg_hero_glass,R.string.lg_dock_icons,R.string.lg_motion,R.string.lg_system};
        List<List<View>> groups=Arrays.asList(wallpaper,glass,dock,motion,system);
        for(int i=0;i<groups.size();i++){
            final int index=i;
            TextView nav=label(a.getString(titles[i]),17);nav.setId(View.generateViewId());nav.setGravity(Gravity.CENTER_VERTICAL);
            nav.setPadding(dp(12),0,dp(10),0);nav.setFocusable(true);nav.setClickable(true);nav.setBackground(selection());
            LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(48));np.bottomMargin=dp(7);sidebar.addView(nav,np);navigation.add(nav);
            ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setClipToPadding(false);
            LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(22),dp(18),dp(22),dp(22));
            TextView title=label(a.getString(titles[i]),25);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(48));content.addView(title,tp);
            for(View view:groups.get(i)){
                ((ViewGroup)view.getParent()).removeView(view);
                int height=view.getId()==R.id.glassOpacityPreview?dp(64):
                        view instanceof SeekBar?dp(48):view.isFocusable()||view instanceof Switch?dp(56):-2;
                LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,height);vp.topMargin=dp(10);
                content.addView(view,vp);
                if(view instanceof TextView && view.isFocusable()){
                    view.setBackground(selection());view.setPadding(dp(16),0,dp(16),0);((TextView)view).setTextSize(17);
                }
                view.setNextFocusLeftId(nav.getId());
            }
            scroll.addView(content);pages.addView(scroll);
            nav.setOnFocusChangeListener((v,on)->{if(on)show(index);});
            nav.setOnClickListener(v->enter());
            nav.setOnKeyListener((v,key,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
                if(key==KeyEvent.KEYCODE_DPAD_RIGHT){enter();return true;}
                if(key==KeyEvent.KEYCODE_DPAD_LEFT)return true;
                return key==KeyEvent.KEYCODE_DPAD_UP&&index==0 || key==KeyEvent.KEYCODE_DPAD_DOWN&&index==navigation.size()-1;
            });
        }
        a.setContentView(shell);show(0);
    }
    private void show(int index){selected=index;pages.setDisplayedChild(index);for(int n=0;n<navigation.size();n++)navigation.get(n).setSelected(n==index);}
    void focusNavigation(){navigation.get(selected).requestFocus();}
    boolean backToNavigation(){if(navigation.get(selected).hasFocus())return false;focusNavigation();return true;}
    private void enter(){View first=firstFocusable(pages.getCurrentView());if(first!=null)first.requestFocus();}
    private View firstFocusable(View view){
        if(view.getVisibility()!=View.VISIBLE||!view.isEnabled())return null;
        if(view.isFocusable() && !(view instanceof ScrollView))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View result=firstFocusable(group.getChildAt(i));if(result!=null)return result;}}
        return null;
    }
    private View before(View v){ViewGroup p=(ViewGroup)v.getParent();return p.getChildAt(p.indexOfChild(v)-1);}
    private View after(View v){ViewGroup p=(ViewGroup)v.getParent();return p.getChildAt(p.indexOfChild(v)+1);}
    private TextView label(String value,int size){TextView t=new TextView(activity);t.setText(value);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setTypeface(UiFont.get(activity,R.string.font_regular));return t;}
    private GradientDrawable panel(){GradientDrawable g=new GradientDrawable();g.setColor(0xff050708);g.setCornerRadius(dp(28));g.setStroke(dp(1),0xff20282e);return g;}
    private StateListDrawable selection(){StateListDrawable s=new StateListDrawable();s.addState(new int[]{android.R.attr.state_focused},shape(0xff444c52,0xffb8c5d3));s.addState(new int[]{android.R.attr.state_selected},shape(0xff252e36,0xff4a5968));s.addState(new int[]{},shape(0xff11181e,0xff1b252d));return s;}
    private GradientDrawable shape(int color,int edge){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(16));g.setStroke(dp(1),edge);return g;}
    private int dp(float value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}
