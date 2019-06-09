package com.nasa.bt.crypt;

/**
 * 加密算法模块工厂类
 * @author QZero
 */
public class CryptModuleFactory {

    /**
     * 当前与服务器通信使用的加密模块
     */
    public static final String CURRENT_CRYPT_MODULE="NONE";

    /**
     * 当前与聊天使用的加密模块
     */
    public static final String CURRENT_CRYPT_MODULE_CHAT="RSA";

    /**
     * 不加密，明文传输
     */
    public static final String MODULE_NONE="NONE";

    /**
     * 进行AES加密传输
     */
    public static final String MODULE_AES="AES";

    /**
     * 使用RSA加密传输
     */
    public static final String MODULE_RSA="RSA";

    /**
     * 生产加密模块
     * @param name 模块名（以本类中的常量为准，忽略大小写）
     * @return 加密模块，不存在则返回null
     */
    public static CryptModule getCryptModule(String name){

        if(name.equalsIgnoreCase(MODULE_NONE))
            return new CryptModuleNone();
        else if(name.equalsIgnoreCase(MODULE_RSA))
            return new CryptModuleRSA();

        return null;
    }


}
