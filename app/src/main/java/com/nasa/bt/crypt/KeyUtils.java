package com.nasa.bt.crypt;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.utils.FileIOUtils;

import java.io.File;

public class KeyUtils {

    private static KeyUtils instance;
    private static Context context;

    private static final String KEY_STORE_FILE_NAME="keyPairs.data";

    private RSAUtils rsaUtils;

    private KeyUtils() {
        File keyFile=new File(context.getFilesDir(),KEY_STORE_FILE_NAME);
        byte[] keyBuf= FileIOUtils.readFile(keyFile);
        if(keyBuf==null){
            genKeySet();
            saveKeySet();
        }else{
            RSAKeySet keySet=JSON.parseObject(new String(keyBuf),RSAKeySet.class);
            rsaUtils=new RSAUtils(keySet);
        }
    }

    public void genKeySet(){
        rsaUtils=new RSAUtils();
    }


    public boolean saveKeySet(){
        if(rsaUtils==null)
            return false;

        RSAKeySet keySet=rsaUtils.getKeySet();
        rsaUtils.loadKeySet(keySet);
        String keyJSON= JSON.toJSONString(keySet);
        File keyFile=new File(context.getFilesDir(),KEY_STORE_FILE_NAME);
        return FileIOUtils.writeFile(keyFile,keyJSON.getBytes());
    }

    public String getPub(){
        return rsaUtils.getKeySet().getPub();
    }

    public String getPri(){
        return rsaUtils.getKeySet().getPri();
    }

    public static void initContext(Context appContext){
        context=appContext;
    }

    public static KeyUtils getInstance(){
        if(instance==null){
            instance=new KeyUtils();
        }
        return instance;
    }

}
