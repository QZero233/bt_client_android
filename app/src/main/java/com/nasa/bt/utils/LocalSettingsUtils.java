package com.nasa.bt.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalSettingsUtils {

    public static final String FILE_NAME="localSettings";

    public static final String FIELD_UID="uid";
    public static final String FIELD_NAME="name";
    public static final String FIELD_CODE_HASH="codeHash";
    public static final String FIELD_SERVER_IP="serverIp";

    public static final String FIELD_NAME_LAST="nameLast";
    public static final String FIELD_CODE_LAST="codeLast";

    public static final String FIELD_CURRENT_KEY_SET_INDEX="currentKeySetIndex";

    public static final String FIELD_FORCE_CA="forceCA";

    public static final String FIELD_LAST_SYNC_TIME="lastSyncTime";


    public static boolean save(Context context,String field, String value){
        SharedPreferences sharedPreferences=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        return sharedPreferences.edit().putString(field,value).commit();
    }

    public static boolean saveInt(Context context,String field,int value){
        SharedPreferences sharedPreferences=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        return sharedPreferences.edit().putInt(field,value).commit();
    }

    public static boolean saveBoolean(Context context,String field,boolean value){
        SharedPreferences sharedPreferences=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        return sharedPreferences.edit().putBoolean(field,value).commit();
    }

    public static boolean saveLong(Context context,String field,long value){
        SharedPreferences sharedPreferences=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        return sharedPreferences.edit().putLong(field,value).commit();
    }

    public static String read(Context context,String field){
        SharedPreferences sharedPreferences=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        return sharedPreferences.getString(field,null);
    }

    public static int readInt(Context context,String field){
        SharedPreferences sharedPreferences=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        return sharedPreferences.getInt(field,0);
    }

    public static boolean readBoolean(Context context,String field){
        SharedPreferences sharedPreferences=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(field,false);
    }

    public static long readLong(Context context,String field){
        SharedPreferences sharedPreferences=context.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE);
        return sharedPreferences.getLong(field,0);
    }

}
