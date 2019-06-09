package com.nasa.bt.crypt;

import java.util.Map;

public class CryptModuleRSA implements CryptModule {

    /**
     * 根据使用场景，加密使用公钥
     * @param clearText 明文（base64编码后再转为byte数组形式）
     * @param key 密钥 别人的公钥
     * @param params 参数 用不着
     * @return 密文，base64编码后转为byte数组形式
     */
    @Override
    public byte[] doEncrypt(byte[] clearText, String key, Map<String, Object> params) {
        try {
            RSAUtils rsaUtils=new RSAUtils(key);
            String result=rsaUtils.publicEncrypt(new String(clearText));
            return result.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 根据使用场景，解密使用公私钥
     * @param cipherText 密文（base64编码后再转为byte数组形式）
     * @param key 密钥 多余的
     * @param params 参数 用不着
     * @return 明文，byte数组形式
     */
    @Override
    public byte[] doDecrypt(byte[] cipherText, String key, Map<String, Object> params) {
        try {
            KeyUtils keyUtils=KeyUtils.getInstance();
            RSAUtils rsaUtils=keyUtils.getRsaUtils();

            String result=rsaUtils.privateDecrypt(new String(cipherText));
            return result.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
