package com.nasa.bt.crypt;

import java.util.Map;

/**
 * 加密算法通用接口
 * @author QZero
 */
public interface CryptModule {

    /**
     *加密操作
     * @param clearText 明文（字节数组形式）
     * @param key 密钥
     * @param params 参数（详情见具体加密算法的注释）
     * @return 加密操作的结果，失败返回null
     */
    byte[] doEncrypt(byte[] clearText, String key, Map<String, Object> params);

    /**
     * 解密操作
     * @param cipherText 密文（字节数组形式）
     * @param key 密钥
     * @param params 参数（详情见具体加密算法的注释）
     * @return 解密操作的结果，失败返回null
     */
    byte[] doDecrypt(byte[] cipherText, String key, Map<String, Object> params);
}
