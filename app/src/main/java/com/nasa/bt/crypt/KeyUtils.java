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

    private KeyUtils() throws Exception{
        File keyFile=new File(context.getFilesDir(),KEY_STORE_FILE_NAME);
        byte[] keyBuf= FileIOUtils.readFile(keyFile);
        if(keyBuf==null){
            genKeySet();
            saveKeySet();
        }else{
            RSAKeySet keySet=JSON.parseObject(new String(keyBuf),RSAKeySet.class);
            rsaUtils=new RSAUtils(keySet.getPub(),keySet.getPri());
        }
    }

    public void genKeySet() throws Exception{
        rsaUtils=new RSAUtils();
    }

    public RSAUtils getRsaUtils() {
        return rsaUtils;
    }

    public boolean saveKeySet(){
        if(rsaUtils==null)
            return false;
        RSAKeySet keySet=new RSAKeySet(rsaUtils.getPub(),rsaUtils.getPri());
        String keyJSON= JSON.toJSONString(keySet);
        File keyFile=new File(context.getFilesDir(),KEY_STORE_FILE_NAME);
        return FileIOUtils.writeFile(keyFile,keyJSON.getBytes());
    }

    public String getPub(){
        return rsaUtils.getPub();
    }

    public String getPri(){
        return rsaUtils.getPri();
    }

    public static void initContext(Context appContext){
        context=appContext;
    }

    public static KeyUtils getInstance() throws Exception{
        if(instance==null){
            instance=new KeyUtils();
        }
        return instance;
    }

}
