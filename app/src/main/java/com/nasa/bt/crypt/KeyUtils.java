package com.nasa.bt.crypt;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.utils.FileIOUtils;

import java.io.File;

public class KeyUtils {

    private static Context context;
    private static RSAKeySet currentKeySet;

    private static final String KEY_STORE_FILE_NAME = "keyPairs.data";


    public static RSAKeySet genKeySet() {
        return RSAUtils.genRSAKeySet();
    }


    public static boolean saveKeySet(RSAKeySet keySet) {
        if (keySet == null)
            return false;

        currentKeySet = keySet;

        String keyJSON = JSON.toJSONString(keySet);
        File keyFile = new File(context.getFilesDir(), KEY_STORE_FILE_NAME);
        return FileIOUtils.writeFile(keyFile, keyJSON.getBytes());
    }

    public static RSAKeySet getCurrentKeySet() {
        return currentKeySet;
    }

    public static void initContext(Context appContext) {
        context = appContext;

        File keyFile = new File(context.getFilesDir(), KEY_STORE_FILE_NAME);
        byte[] keyBuf = FileIOUtils.readFile(keyFile);
        if (keyBuf == null || JSON.parseObject(new String(keyBuf), RSAKeySet.class) == null) {
            RSAKeySet rsaKeySet = genKeySet();
            saveKeySet(rsaKeySet);
        } else {
            RSAKeySet keySet = JSON.parseObject(new String(keyBuf), RSAKeySet.class);
            currentKeySet = keySet;
        }
    }

}
