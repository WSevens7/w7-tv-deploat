package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

/** Google Material Icons Round vectors bundled with Android SDK; never hand-drawn glyphs. */
final class MaterialIcons {
    private MaterialIcons() {}
    static Drawable get(Context context, String name, int size) {
        int resource;
        switch (name) {
            case "wifi": resource=R.drawable.ic_wifi; break;
            case "wallpaper": resource=R.drawable.ic_material_wallpaper; break;
            case "input": resource=R.drawable.ic_input; break;
            case "picture": resource=R.drawable.ic_picture; break;
            case "clock": resource=R.drawable.ic_material_clock; break;
            case "moon": resource=R.drawable.ic_material_moon; break;
            case "close": resource=R.drawable.ic_material_close; break;
            case "apps": resource=R.drawable.ic_apps; break;
            case "sound": resource=R.drawable.ic_material_sound; break;
            case "appearance": resource=R.drawable.ic_material_appearance; break;
            case "sun": resource=R.drawable.ic_material_brightness; break;
            default: resource=R.drawable.ic_settings;
        }
        Drawable icon=context.getDrawable(resource).mutate();
        icon.setTint(Color.WHITE);
        icon.setBounds(0,0,size,size);
        return icon;
    }
}
