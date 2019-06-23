package com.nasa.bt.utils;

import android.content.Context;
import android.text.TextUtils;

import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.loop.MessageLoopService;

public class LocalDbUtils {

    private static String getParam(Context context){
        String ip=LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_SERVER_IP);
        if(TextUtils.isEmpty(ip))
            ip= MessageLoopService.SERVER_IP_DEFAULT;
        return ip;
    }

    public static CommonDbHelper getMsgHelper(Context context){
        return new CommonDbHelper(context, Msg.class,getParam(context));
    }

    public static CommonDbHelper getUserInfoHelper(Context context){
        return new CommonDbHelper(context, UserInfo.class,getParam(context));
    }


}
