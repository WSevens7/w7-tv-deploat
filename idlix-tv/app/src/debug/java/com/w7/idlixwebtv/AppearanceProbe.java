package com.w7.idlixwebtv;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/** Device integration check: the signed companion can read, but cannot change, launcher preferences. */
public final class AppearanceProbe extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            ContentResolver resolver=getTargetContext().getContentResolver();
            Uri uri=Uri.parse("content://com.gathin.tvhome.appearance/opacity");
            try(Cursor c=resolver.query(uri,new String[]{"opacity"},null,null,null)){
                if(c==null||!c.moveToFirst()||c.getColumnCount()!=1)throw new AssertionError("Missing single-value contract");
                int value=c.getInt(c.getColumnIndexOrThrow("opacity"));
                if(value<0||value>100)throw new AssertionError("Out of range");
                result.putInt("opacity",value);
            }
            try{resolver.query(Uri.parse("content://com.gathin.tvhome.appearance/private"),null,null,null,null);throw new AssertionError("Unknown path accepted");}catch(IllegalArgumentException expected){}
            try{resolver.insert(uri,new ContentValues());throw new AssertionError("Insert accepted");}catch(UnsupportedOperationException expected){}
            try{resolver.update(uri,new ContentValues(),null,null);throw new AssertionError("Update accepted");}catch(UnsupportedOperationException expected){}
            try{resolver.delete(uri,null,null);throw new AssertionError("Delete accepted");}catch(UnsupportedOperationException expected){}
            result.putString("result","PASS: read + unknown path + insert/update/delete rejection");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable failure){result.putString("failure",failure.toString());finish(Activity.RESULT_CANCELED,result);}
    }
}
