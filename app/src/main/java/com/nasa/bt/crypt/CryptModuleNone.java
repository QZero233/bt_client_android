package com.nasa.bt.crypt;

import java.util.Map;

/**
 * 不进行任何加密的加密模块
 * @author QZero
 */
public class CryptModuleNone implements CryptModule {

    CryptModuleNone(){

    }

    @Override
    public byte[] doEncrypt(byte[] clearText, String key, Map<String, Object> params) {
        return clearText;
    }

    @Override
    public byte[] doDecrypt(byte[] cipherText, String key, Map<String, Object> params) {
        return cipherText;
    }
}
