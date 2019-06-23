package com.nasa.bt;

import com.nasa.bt.crypt.AESUtils;

import org.junit.Test;

public class AESTest {

    @Test
    public void testAES(){
        String cipher= AESUtils.aesEncrypt("233","key");
        String clear=AESUtils.aesDecrypt(cipher,"key");
        System.out.println(cipher+"\n"+clear);
    }

}
