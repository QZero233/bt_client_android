package com.nasa.bt.utils;

import android.content.Context;
import android.text.TextUtils;

import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.Session;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.loop.MessageLoopService;

public class LocalDbUtils {

    private static String getParam(Context context){
        String ip=LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_SERVER_IP);
        String name=LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_NAME);
        if(TextUtils.isEmpty(ip))
            ip= MessageLoopService.SERVER_IP_DEFAULT;
        if(TextUtils.isEmpty(name))
            name="";
        return ip+name;
    }

    public static CommonDbHelper getMsgHelper(Context context){
        return new CommonDbHelper(context, Msg.class,getParam(context));
    }

    public static CommonDbHelper getUserInfoHelper(Context context){
        return new CommonDbHelper(context, UserInfo.class,getParam(context));
    }


    public static CommonDbHelper getSessionHelper(Context context){
        return new CommonDbHelper(context, Session.class,getParam(context));
    }
}
