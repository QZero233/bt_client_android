package com.nasa.bt;

import android.util.Base64;

import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AESAndroidTest {
    static Logger logger= AppLogConfigurator.getLogger();

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
            byte[] enCodeFormat = getAESKey(pwd);
            logger.debug(Arrays.toString(enCodeFormat));

            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
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
            byte[] enCodeFormat = getAESKey(pwd);
            logger.debug(Arrays.toString(enCodeFormat));

            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] result=cipher.doFinal(encrypted);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void testAndroidAES(){
        String cipher=aesEncrypt("wdnmd","key");

        logger.debug(cipher);
        logger.debug(aesDecrypt(cipher,"key"));
    }
}
