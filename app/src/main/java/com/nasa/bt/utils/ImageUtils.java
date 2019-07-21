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


        return -1;
    }

}
