package com.nasa.bt.upgrade;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.MainActivity;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpgradeUtils {

    private static final String UPGRADE_FILE_URL="https://github.com/QZero233/bt_client_android_upgrade/releases/download/1/upgrade.json";
    private static final Logger log= AppLogConfigurator.getLogger();

    public static String doGet(String u){
        try {
            URL url = new URL(u);
            URLConnection connection = url.openConnection();
            InputStream in = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(in,"utf-8");
            BufferedReader br = new BufferedReader(isr);
            String line;
            StringBuilder sb = new StringBuilder();
            while((line = br.readLine()) != null)
            {
                sb.append(line);
            }
            br.close();
            isr.close();
            in.close();
            return sb.toString();
        }catch (Exception e){
            log.error("发送GET请求时异常",e);
            return null;
        }
    }

    private static int getVersionCode(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return 0;

    }

    private static UpgradeStatus getUpgradeStatus(){
        String json=doGet(UPGRADE_FILE_URL);
        if(TextUtils.isEmpty(json))
            return null;
        return JSON.parseObject(json,UpgradeStatus.class);
    }

    /**
     * 检查是否需要更新
     */
    public static void checkUpgrade(final Context context,final Handler handler){



        new Thread(){
            @Override
            public void run() {
                super.run();

                UpgradeStatus upgradeStatus=getUpgradeStatus();
                if(upgradeStatus==null){
                    handler.sendEmptyMessage(0);
                    return;
                }


                int currentVerCode=getVersionCode(context);
                if(currentVerCode>=upgradeStatus.getNewestVerCode()){
                    handler.sendEmptyMessage(0);
                    return;
                }

                Message msg=new Message();
                msg.what=1;
                msg.obj=upgradeStatus;
                handler.sendMessage(msg);
            }
        }.start();

    }

}
