package com.nasa.bt;


import org.junit.Test;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class NewAESTest {
    public static String aesEncrypt(String clear,String pwd){
        return Base64.getEncoder().encodeToString(aesEncrypt(clear.getBytes(),pwd));
    }
    public static String aesDecrypt(String encrypted,String pwd){
        return new String(aesDecrypt(Base64.getDecoder().decode(encrypted),pwd));
    }

    public static byte[] aesEncrypt(byte[] clear,String pwd){
        try{
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(256, new SecureRandom(pwd.getBytes()));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();

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
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(256, new SecureRandom(pwd.getBytes()));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();

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
    public void testAES(){
        String cipher=aesEncrypt("wdnmd","key");
        System.out.println(cipher);
        System.out.println(aesDecrypt(cipher,"key"));

    }
}
