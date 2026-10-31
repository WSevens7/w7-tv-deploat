package com.gathin.tvhome;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/** Read-only appearance contract for companion apps signed by the same owner. */
public final class AppearanceProvider extends ContentProvider {
    static Uri opacityUri(Context context) {
        return Uri.parse("content://"+context.getPackageName()+".appearance/opacity");
    }
    private void requireOpacity(Uri uri) {
        if(!opacityUri(getContext()).equals(uri))throw new IllegalArgumentException("Unknown appearance setting");
    }
    @Override public boolean onCreate(){return true;}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder){
        requireOpacity(uri);
        MatrixCursor result=new MatrixCursor(new String[]{"opacity"},1);
        result.addRow(new Object[]{LiquidGlass.opacity(getContext())});
        result.setNotificationUri(getContext().getContentResolver(),opacityUri(getContext()));
        return result;
    }
    @Override public String getType(Uri uri){requireOpacity(uri);return "vnd.android.cursor.item/vnd.w7.glass-opacity";}
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException("Read only");}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException("Read only");}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException("Read only");}
}
