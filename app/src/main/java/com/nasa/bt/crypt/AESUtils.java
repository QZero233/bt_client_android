package com.nasa.bt.crypt;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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

    public static byte[] aesEncryptWithPwdHash(byte[] buf,String key){
        byte[] result = AESUtils.aesEncrypt(buf, key);
        byte[] pwdHash=SHA256Utils.getSHA256InByteArray(key.getBytes());
        int i=pwdHash.length;

        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        try {
            baos.write(pwdHash);
            baos.write(result);
            return baos.toByteArray();
        }catch (Exception e){
            return null;
        }
    }

    public static byte[] aesDecryptWithPwdHash(byte[] buf,String key){
        ByteArrayInputStream bais=new ByteArrayInputStream(buf);

        if(buf==null)
            return null;

        byte[] pwdHash=new byte[32];
        bais.read(pwdHash,0,32);
        if(pwdHash==null)
            return null;

        if(!Base64.encodeToString(pwdHash,Base64.DEFAULT).equals(SHA256Utils.getSHA256InBase64(key)))
            return null;


        byte[] keyEncrypted=new byte[buf.length-32];
        bais.read(keyEncrypted,0,keyEncrypted.length);
        if(keyEncrypted==null)
            return null;

        if((buf=AESUtils.aesDecrypt(keyEncrypted,key))==null)
            return null;


        return buf;
    }


}
