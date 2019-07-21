package com.nasa.bt.utils;

import android.text.TextUtils;

import com.nasa.bt.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageUtils {

    public static Map<String, List<String>> images=new HashMap<>();
    static{
        List<String> hj=new ArrayList<>();
        hj.add("手动滑稽");
        hj.add("问号滑稽");
        hj.add("党员滑稽");
        images.put("滑稽系列",hj);

        List<String> bz=new ArrayList<>();
        bz.add("国旗");
        bz.add("党旗");
        bz.add("北社");
        bz.add("NASA");
        images.put("标志系列",bz);

        List<String> cxk=new ArrayList<>();
        cxk.add("鸡你太美");
        for(int i=2;i<=12;i++){
            cxk.add("鸡你太美"+i);
        }
        images.put("CXK系列",cxk);

        List<String> normal=new ArrayList<>();
        normal.add("傲娇地瞄你一眼");
        normal.add("用扇子遮住嘴显得自己很萌");
        normal.add("抱着枕头装出一副要睡觉的样子");
        normal.add("发问号也要显得可爱");
        normal.add("只是装傻");
        normal.add("要被你气哭了");
        normal.add("虽说托腮疑问只要加几个问号就可以了但是这个表情包却要带上一副眼镜显得更有文艺气息");
        normal.add("奇奇怪怪的眼神");
        normal.add("被你气的吐血了哟");
        normal.add("尴尬这两字都写在表情上的表情包应该更能表达此时我的心情吧");
        normal.add("你笑诚哥死的早，诚哥笑你RI的少");
        normal.add("我哭了，你开心了噻");
        normal.add("哎哟，不错哟");
        normal.add("即使不明物体遮住了我明亮的双眼我也依然会昂头向诗和远方");
        normal.add("？，？？");
        normal.add("吐舌");
        normal.add("我歪歪地一笑");
        normal.add("吐舌求抱抱");
        normal.add("我打伞的时候别人都觉得我是个沙雕");
        normal.add("生无可恋");
        images.put("日常表情包",normal);
    }

    public static int textToImage(String text) {
        if (TextUtils.isEmpty(text))
            return -1;
        if (text.equals("手动滑稽"))
            return R.mipmap.sdhj;
        if (text.equals("党员滑稽"))
            return R.mipmap.dyhj;
        if (text.equals("党旗"))
            return R.mipmap.cpc;
        if (text.equals("国旗"))
            return R.mipmap.prc;
        if (text.equals("北社"))
            return R.mipmap.nacp;
        if (text.equals("问号滑稽"))
            return R.mipmap.whhj;
        if (text.equalsIgnoreCase("NASA"))
            return R.mipmap.nasa;
        if (text.equals("鸡你太美"))
            return R.mipmap.jntm1;
        if (text.equals("鸡你太美2"))
            return R.mipmap.jntm2;
        if (text.equals("鸡你太美3"))
            return R.mipmap.jntm3;
        if (text.equals("鸡你太美4"))
            return R.mipmap.jntm4;
        if (text.equals("鸡你太美5"))
            return R.mipmap.jntm5;
        if (text.equals("鸡你太美6"))
            return R.mipmap.jntm6;
        if (text.equals("鸡你太美7"))
            return R.mipmap.jntm7;
        if (text.equals("鸡你太美8"))
            return R.mipmap.jntm8;
        if (text.equals("鸡你太美9"))
            return R.mipmap.jntm9;
        if (text.equals("鸡你太美10"))
            return R.mipmap.jntm10;
        if (text.equals("鸡你太美11"))
            return R.mipmap.jntm11;
        if (text.equals("鸡你太美12"))
            return R.mipmap.jntm12;
        if(text.equals("傲娇地瞄你一眼"))
            return R.mipmap.normal1;
        if(text.equals("用扇子遮住嘴显得自己很萌"))
            return R.mipmap.normal2;
        if(text.equals("抱着枕头装出一副要睡觉的样子"))
            return R.mipmap.normal3;
        if(text.equals("发问号也要显得可爱"))
            return R.mipmap.normal4;
        if(text.equals("只是装傻"))
            return R.mipmap.normal5;
        if(text.equals("要被你气哭了"))
            return R.mipmap.normal6;
        if(text.equals("虽说托腮疑问只要加几个问号就可以了但是这个表情包却要带上一副眼镜显得更有文艺气息"))
            return R.mipmap.normal7;
        if(text.equals("奇奇怪怪的眼神"))
            return R.mipmap.normal8;
        if(text.equals("被你气的吐血了哟"))
            return R.mipmap.normal9;
        if(text.equals("尴尬这两字都写在表情上的表情包应该更能表达此时我的心情吧"))
            return R.mipmap.normal10;
        if(text.equals("你笑诚哥死的早，诚哥笑你RI的少"))
            return R.mipmap.normal11;
        if(text.equals("我哭了，你开心了噻"))
            return R.mipmap.normal12;
        if(text.equals("哎哟，不错哟"))
            return R.mipmap.normal13;
        if(text.equals("即使不明物体遮住了我明亮的双眼我也依然会昂头向诗和远方"))
            return R.mipmap.normal14;
        if(text.equals("？，？？"))
            return R.mipmap.normal15;
        if(text.equals("吐舌"))
            return R.mipmap.normal16;
        if(text.equals("我歪歪地一笑"))
            return R.mipmap.normal17;
        if(text.equals("吐舌求抱抱"))
            return R.mipmap.normal18;
        if(text.equals("我打伞的时候别人都觉得我是个沙雕"))
            return R.mipmap.normal19;
        if(text.equals("生无可恋"))
            return R.mipmap.normal20;








        return -1;
    }

}
