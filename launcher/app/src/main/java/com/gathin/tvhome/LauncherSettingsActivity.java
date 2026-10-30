package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Switch;
import android.view.ViewGroup;
import android.view.Gravity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;
    private TextView wallpaperIntervalChoice, opacityPreview;
    private SeekBar opacitySlider;
    private final ExecutorService thumbnails = Executors.newSingleThreadExecutor();
    private volatile int previewGeneration;
    private AppearanceLayout appearanceLayout;
    private Switch wallpaperAuto;
    private boolean refreshingSettings;
    private static final int[] INTERVALS = {0,1,5,15,30,60,180,360,1440};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        appearanceLayout=AppearanceLayout.install(this);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = LiquidGlass.opacity(this);
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                LiquidGlass.setOpacity(this, value);
                refresh();
            });
        }
        // Keep the numeric choices increasing in the Hebrew reading direction. Reorder the
        // actual views so D-pad focus follows their positions without changing the saved values.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        wallpaperChoice.setCompoundDrawables(MaterialIcons.get(this,"wallpaper",dp(24)),null,null,null);
        wallpaperChoice.setCompoundDrawablePadding(dp(12));
        wallpaperIntervalChoice = findViewById(R.id.wallpaperIntervalChoice);
        wallpaperIntervalChoice.setOnClickListener(v -> showWallpaperInterval());
        wallpaperAuto=findViewById(R.id.wallpaperAutoSwitch);
        wallpaperAuto.setOnCheckedChangeListener((button,on)->{
            if(refreshingSettings)return;
            int last=getSharedPreferences(Config.LIQUID_PREFS,MODE_PRIVATE).getInt("last_wallpaper_interval",5);
            setWallpaperInterval(on?Math.max(1,last):0);
        });
        opacityPreview = findViewById(R.id.glassOpacityPreview);
        opacityPreview.setBackground(new LiquidGlass(opacityPreview,18,false));
        opacitySlider = findViewById(R.id.glassOpacitySlider);
        opacitySlider.setKeyProgressIncrement(5);
        opacitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int value,boolean user){
                if(!user)return;
                dockOpacity=value;
                LiquidGlass.setOpacity(LauncherSettingsActivity.this,value);
                refresh();
            }
            @Override public void onStartTrackingTouch(SeekBar bar){}
            @Override public void onStopTrackingTouch(SeekBar bar){}
        });
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(OpenOnStart.KEY).commit();
            getSharedPreferences(Config.LIQUID_PREFS,MODE_PRIVATE).edit()
                    .remove(Config.WALLPAPER_INTERVAL_KEY).remove(Config.PENDING_WALLPAPER_KEY).apply();
            LiquidGlass.resetOpacity(this);
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = LiquidGlass.opacity(this);
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        appearanceLayout.focusNavigation();
    }

    @Override public void onBackPressed(){if(!appearanceLayout.backToNavigation())super.onBackPressed();}

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        if(!"none".equals(current)) current=getSharedPreferences("wallpaper_rotation",MODE_PRIVATE).getString("image",current);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        final int selected=checked;
        final int generation=++previewGeneration;
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,0,labels){
            @Override public View getView(int position,View reuse,ViewGroup parent){
                LinearLayout row;
                if(reuse instanceof LinearLayout)row=(LinearLayout)reuse;
                else{
                    row=new LinearLayout(LauncherSettingsActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(12),dp(6),dp(12),dp(6));
                    ImageView image=new ImageView(LauncherSettingsActivity.this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    row.addView(image,new LinearLayout.LayoutParams(dp(96),dp(54)));
                    TextView label=new TextView(LauncherSettingsActivity.this);label.setTextSize(15);label.setTextColor(Color.WHITE);
                    label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(14);row.addView(label,lp);
                    RadioButton check=new RadioButton(LauncherSettingsActivity.this);check.setFocusable(false);check.setClickable(false);row.addView(check);
                }
                ImageView image=(ImageView)row.getChildAt(0);TextView name=(TextView)row.getChildAt(1);
                ((RadioButton)row.getChildAt(2)).setChecked(position==selected);name.setText(labels[position]);
                image.setImageDrawable(null);image.setBackgroundColor(0x44202A38);
                image.setTag(position<images.size()?images.get(position).getPath():null);
                if(position<images.size()){
                    File file=images.get(position);
                    thumbnails.execute(()->{
                        if(generation!=previewGeneration)return;
                        Bitmap thumb=thumbnail(file);
                        image.post(()->{
                            if(thumb==null)return;
                            if(isDestroyed()||generation!=previewGeneration||!file.getPath().equals(image.getTag()))thumb.recycle();
                            else image.setImageBitmap(thumb);
                        });
                    });
                }
                return row;
            }
        };
        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(adapter, checked, (picker, which) -> {
                    picker.dismiss();
                    SharedPreferences liquid=getSharedPreferences(Config.LIQUID_PREFS,MODE_PRIVATE);
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                        liquid.edit().putInt(Config.WALLPAPER_INTERVAL_KEY,0).remove(Config.PENDING_WALLPAPER_KEY).apply();
                    } else {
                        int interval=Config.load(this).wallpaperRotationMinutes;
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                        liquid.edit().putInt(Config.WALLPAPER_INTERVAL_KEY,interval)
                                .putString(Config.PENDING_WALLPAPER_KEY,images.get(which).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (picker, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    getSharedPreferences(Config.LIQUID_PREFS,MODE_PRIVATE).edit()
                            .remove(Config.WALLPAPER_INTERVAL_KEY).remove(Config.PENDING_WALLPAPER_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnDismissListener(dismissed->previewGeneration++);
        dialog.show();
    }

    private void showWallpaperInterval(){
        int current=Config.load(this).wallpaperRotationMinutes;
        String[] labels=new String[INTERVALS.length];int selected=-1;
        for(int i=0;i<INTERVALS.length;i++){labels[i]=intervalLabel(INTERVALS[i]);if(INTERVALS[i]==current)selected=i;}
        new AlertDialog.Builder(this).setTitle(R.string.lg_wallpaper_interval)
                .setSingleChoiceItems(labels,selected,(dialog,which)->{
                    setWallpaperInterval(INTERVALS[which]);dialog.dismiss();
                }).setNegativeButton(android.R.string.cancel,null).show();
    }

    private void setWallpaperInterval(int minutes){
        SharedPreferences liquid=getSharedPreferences(Config.LIQUID_PREFS,MODE_PRIVATE);
        String image=liquid.getString(Config.PENDING_WALLPAPER_KEY,
                getSharedPreferences("wallpaper_rotation",MODE_PRIVATE).getString("image",null));
        SharedPreferences.Editor edit=liquid.edit().putInt(Config.WALLPAPER_INTERVAL_KEY,minutes);
        if(image!=null)edit.putString(Config.PENDING_WALLPAPER_KEY,image);
        if(minutes>0)edit.putInt("last_wallpaper_interval",minutes);
        edit.apply();
        if(minutes==0&&image!=null)choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,image).apply();
        else if(minutes>0&&"none".equals(choices.getString(Config.WALLPAPER_CHOICE_KEY,null)))choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
        changed();refresh();
    }

    private String intervalLabel(int minutes){return minutes==0?getString(R.string.lg_rotation_off):minutes%60==0?getString(R.string.lg_every_hours,minutes/60):getString(R.string.lg_every_minutes,minutes);}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private static Bitmap thumbnail(File file){
        try{
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),o);
            if(o.outWidth<=0||o.outHeight<=0)return null;
            o.inSampleSize=1;while(o.outWidth/o.inSampleSize>384||o.outHeight/o.inSampleSize>216)o.inSampleSize*=2;
            o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(file.getPath(),o);
        }catch(Exception|OutOfMemoryError invalid){return null;}
    }
    @Override protected void onDestroy(){previewGeneration++;thumbnails.shutdownNow();super.onDestroy();}

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        refreshingSettings=true;
        int interval=Config.load(this).wallpaperRotationMinutes;
        wallpaperAuto.setChecked(interval>0);
        wallpaperIntervalChoice.setText(getString(R.string.lg_rotation_state,intervalLabel(interval)));
        wallpaperIntervalChoice.setEnabled(interval>0);wallpaperIntervalChoice.setAlpha(interval>0?1f:.5f);
        opacityPreview.setText(getString(R.string.lg_opacity_preview,dockOpacity));
        if(opacitySlider.getProgress()!=dockOpacity)opacitySlider.setProgress(dockOpacity);
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperCurrent
                        : getString(R.string.wallpaper_theme_default)));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        followThemeChoice.setSelected(!hasOnTvChoices());
        refreshingSettings=false;
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(OpenOnStart.KEY)
                || getSharedPreferences(Config.LIQUID_PREFS,MODE_PRIVATE).contains(Config.WALLPAPER_INTERVAL_KEY)
                || LiquidGlass.opacity(this)!=100;
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
