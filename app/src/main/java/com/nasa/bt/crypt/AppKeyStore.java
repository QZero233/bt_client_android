package com.nasa.bt.crypt;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.utils.FileIOUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppKeyStore {

    private Context context;
    private RSAKeySet currentKeySet;

    private List<RSAKeySet> keySets=new ArrayList<>();

    private static AppKeyStore instance;

    private static final String KEY_STORE_FILE_NAME = "keySetsForUse.data";

    public static final String KEY_SET_NAME_DEFAULT="default";

    public static final String BACKUP_FILE_NAME="keySetsForUse.data";

    private AppKeyStore(){

    }

    public static AppKeyStore getInstance(){
        if(instance==null)
            instance=new AppKeyStore();
        return instance;
    }

    public void initKeyStore(Context context){
        this.context=context;
        load();
    }


    public boolean backup(String pwd){
        File backupFile=new File(Environment.getExternalStorageDirectory(),BACKUP_FILE_NAME);
        byte[] result=AESUtils.aesEncryptWithPwdHash(keySetsToString(keySets).getBytes(),pwd);
        if(result==null)
            return false;

        return FileIOUtils.writeFile(backupFile,result);
    }

    public boolean recovery(String pwd){
        File backupFile=new File(Environment.getExternalStorageDirectory(),BACKUP_FILE_NAME);
        byte[] buf=FileIOUtils.readFile(backupFile);
        if(buf==null)
            return false;

        buf=AESUtils.aesDecryptWithPwdHash(buf,pwd);
        if(buf==null)
            return false;

        List<RSAKeySet> keySets=stringToKeySets(new String(buf));
        if(keySets==null)
            return false;
        if(keySets.isEmpty())
            return false;

        this.keySets=keySets;

        switchKeySet(0);

        return save();
    }

    private static List<RSAKeySet> stringToKeySets(String str){
        return JSON.parseArray(str,RSAKeySet.class);
    }

    private static String keySetsToString(List<RSAKeySet>  keySets){
        String keySetString=JSON.toJSONString(keySets);
        return keySetString;
    }

    public void load(){
        File keyFile = new File(context.getFilesDir(), KEY_STORE_FILE_NAME);
        byte[] buf=FileIOUtils.readFile(keyFile);
        if(buf==null){
            init();
            return;
        }
        keySets=stringToKeySets(new String(buf));

        if(keySets==null)
            keySets=new ArrayList<>();
        if(keySets.isEmpty()){
            init();
            return;
        }

        int currentIndex=LocalSettingsUtils.readInt(context,LocalSettingsUtils.FIELD_CURRENT_KEY_SET_INDEX);
        currentKeySet=keySets.get(currentIndex);
        if(currentKeySet==null){
            currentKeySet=RSAUtils.genRSAKeySet();
            keySets.set(currentIndex,currentKeySet);
            save();
        }
    }

    public void init(){
        currentKeySet=RSAUtils.genRSAKeySet();
        currentKeySet.setName(KEY_SET_NAME_DEFAULT);
        keySets.add(currentKeySet);
        save();
        LocalSettingsUtils.saveInt(context,LocalSettingsUtils.FIELD_CURRENT_KEY_SET_INDEX,0);
    }

    public boolean add(RSAKeySet keySet){
        keySets.add(keySet);
        return save();
    }

    public boolean remove(int index){
        keySets.remove(index);
        return save();
    }

    public boolean update(int index,RSAKeySet keySet){
        keySets.set(index,keySet);
        return save();
    }


    public synchronized boolean save(){
        File keyFile = new File(context.getFilesDir(), KEY_STORE_FILE_NAME);
        return FileIOUtils.writeFile(keyFile,keySetsToString(keySets).getBytes());
    }

    public void switchKeySet(int index){
        RSAKeySet keySet=keySets.get(index);
        if(keySet==null)
            return;

        currentKeySet=keySet;
        LocalSettingsUtils.saveInt(context,LocalSettingsUtils.FIELD_CURRENT_KEY_SET_INDEX,index);
    }

    public List<RSAKeySet> getKeySets() {
        return keySets;
    }

    public RSAKeySet getCurrentKeySet() {
        return currentKeySet;
    }

}
