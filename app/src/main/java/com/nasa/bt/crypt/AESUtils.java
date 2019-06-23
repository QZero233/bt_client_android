package com.nasa.bt.crypt;

import android.text.TextUtils;
import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class AESUtils {

    public static String pwdTo16(String pwd){
        if(TextUtils.isEmpty(pwd))
            pwd="";
        String base64=SHA256Utils.getSHA256InHex(pwd);
        return base64.substring(0,16);
    }

    public static String aesEncrypt(String clear,String pwd){
        return Base64.encodeToString(aesEncrypt(clear.getBytes(),pwd),Base64.DEFAULT);
    }
    public static String aesDecrypt(String encrypted,String pwd){
        return new String(aesDecrypt(Base64.decode(encrypted,Base64.DEFAULT),pwd));
    }

    public static byte[] aesEncrypt(byte[] clear,String pwd){
        try{
            SecretKeySpec key = new SecretKeySpec(pwdTo16(pwd).getBytes("UTF-8"), "AES/CBC/PKCS5PADDING");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] result=cipher.doFinal(clear);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    public static byte[] aesDecrypt(byte[] encrypted,String pwd){
        try{
            SecretKeySpec key = new SecretKeySpec(pwdTo16(pwd).getBytes(), "AES/CBC/PKCS5PADDING");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] result=cipher.doFinal(encrypted);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

}
