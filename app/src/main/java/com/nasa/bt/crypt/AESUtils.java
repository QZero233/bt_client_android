package com.nasa.bt.crypt;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class AESUtils {

    public static byte[] getAESKey(String keySource){
        String sha256First= SHA256Utils.getSHA256InBase64(keySource);
        sha256First+=keySource;
        return SHA256Utils.getSHA256InByteArray(sha256First.getBytes());
    }

    public static String aesEncrypt(String clear,String pwd){
        return Base64.encodeToString(aesEncrypt(clear.getBytes(),pwd),Base64.DEFAULT);
    }
    public static String aesDecrypt(String encrypted,String pwd){
        return new String(aesDecrypt(Base64.decode(encrypted,Base64.DEFAULT),pwd));
    }

    public static byte[] aesEncrypt(byte[] clear,String pwd){
        try{
            SecretKeySpec key = new SecretKeySpec(getAESKey(pwd), "AES/CBC/PKCS5PADDING");
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
            SecretKeySpec key = new SecretKeySpec(getAESKey(pwd), "AES/CBC/PKCS5PADDING");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] result=cipher.doFinal(encrypted);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] aesEncrypt(byte[] clear,byte[] pwd){
        try{
            SecretKeySpec key = new SecretKeySpec(pwd, "AES/CBC/PKCS5PADDING");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] result=cipher.doFinal(clear);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] aesDecrypt(byte[] encrypted,byte[] pwd){
        try{
            SecretKeySpec key = new SecretKeySpec(pwd, "AES/CBC/PKCS5PADDING");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] result=cipher.doFinal(encrypted);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static String aesEncrypt(String clear,byte[] pwd){
        return Base64.encodeToString(aesEncrypt(clear.getBytes(),pwd),Base64.DEFAULT);
    }
    public static String aesDecrypt(String encrypted,byte[] pwd){
        return new String(aesDecrypt(Base64.decode(encrypted,Base64.DEFAULT),pwd));
    }

}
