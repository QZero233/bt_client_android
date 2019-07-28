package com.nasa.bt.crypt;

import android.util.Base64;

import com.nasa.bt.cls.RSAKeySet;

import java.util.Map;

public class CryptModuleRSA implements CryptModule {

    private String dstPubKey =null;
    private String myPrivateKey=null;

    /**
     * 根据使用场景，加密使用公钥
     * @param clearText 明文（base64编码后再转为byte数组形式）
     * @param key 密钥 多余的
     * @param params 参数 用不着
     * @return 密文，base64编码后转为byte数组形式
     */
    @Override
    public byte[] doEncrypt(byte[] clearText, String key, Map<String, Object> params) {
        if(dstPubKey ==null)
            return null;

        try {

            clearText= Base64.encode(clearText,Base64.NO_WRAP);

            RSAKeySet keySet=new RSAKeySet(dstPubKey,null);
            RSAUtils rsaUtils=new RSAUtils(keySet);

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
            if(dstPubKey ==null)
                /*if(!new String(cipherText,0,4).equalsIgnoreCase(Datagram.IDENTIFIER_CHANGE_HAND_SHAKE_PARAM)){
                    return null;
                }
                ByteArrayInputStream inputStream=new ByteArrayInputStream(cipherText);
                inputStream.skip(4);
                byte[] keyBuf=new byte[cipherText.length-4];
                inputStream.read(keyBuf);
                dstPubKey=new String(keyBuf);

                return null;*/
                return null;


            if(myPrivateKey==null)
                return null;

            RSAKeySet keySet=new RSAKeySet(null,myPrivateKey);
            RSAUtils rsaUtils=new RSAUtils(keySet);

            String result=rsaUtils.privateDecrypt(new String(cipherText));
            return Base64.decode(result,Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void initKeys(String dstPubKey,String myPriKey){
        this.dstPubKey=dstPubKey;
        this.myPrivateKey=myPriKey;
    }
}
