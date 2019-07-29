package com.nasa.bt.crypt;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.utils.FileIOUtils;

import java.io.File;

public class KeyUtils {

    private static KeyUtils instance;
    private static Context context;

    private static final String KEY_STORE_FILE_NAME="keySetForConnection.data";

    public static RSAKeySet read(){
        File keyFile=new File(context.getFilesDir(),KEY_STORE_FILE_NAME);
        byte[] keyBuf= FileIOUtils.readFile(keyFile);
        if(keyBuf==null)
            return null;
        RSAKeySet keySet=JSON.parseObject(new String(keyBuf),RSAKeySet.class);
        return keySet;
    }

    public static void initContext(Context appContext){
        context=appContext;
        if(read()==null){
            RSAKeySet keySet=RSAUtils.genRSAKeySet();
            save(keySet);
        }
    }

    public static boolean save(RSAKeySet keySet){
        if(keySet==null)
            return false;


        String keyJSON= JSON.toJSONString(keySet);
        File keyFile=new File(context.getFilesDir(),KEY_STORE_FILE_NAME);
        return FileIOUtils.writeFile(keyFile,keyJSON.getBytes());
    }

    public static KeyUtils getInstance(){
        if(instance==null){
            instance=new KeyUtils();
        }
        return instance;
    }

}
