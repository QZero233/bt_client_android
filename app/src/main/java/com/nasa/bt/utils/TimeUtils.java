package com.nasa.bt.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2018/2/7 0007.
 */

public class TimeUtils {
    public static String toStandardTime(long time){
        Date date=new Date(time);
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return format.format(date);
    }
}
