package com.nasa.bt.upgrade;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class UpgradeUtils {

    private static final String TEMP_FILE_NAME="upgradeStatus";
    private static final Logger log= AppLogConfigurator.getLogger();

    public static int getVersionCode(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean writeTempUpgradeStatusFile(Context context,String json){
        File file=new File(context.getFilesDir(),TEMP_FILE_NAME);
        try{
            FileOutputStream fos=new FileOutputStream(file);
            fos.write(json.getBytes());
            fos.close();
            return true;
        }catch (Exception e){
            log.error("写入更新信息时异常",e);
            return false;
        }
    }

    public static UpgradeStatus readTempUpgradeStatusFil(Context context){
        File file=new File(context.getFilesDir(),TEMP_FILE_NAME);
        try{
            FileInputStream fis=new FileInputStream(file);
            byte[] buf=new byte[(int) file.length()];
            fis.read(buf);
            fis.close();
            return JSON.parseObject(buf,UpgradeStatus.class);
        }catch (Exception e){
            log.error("读取更新信息时异常",e);
            return null;
        }
    }

    public static boolean deleteTempUpgradeStatusFil(Context context){
        File file=new File(context.getFilesDir(),TEMP_FILE_NAME);
        if(!file.exists())
            return false;
        return file.delete();
    }

}
