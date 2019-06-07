package com.nasa.bt.cls;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 数据包类
 * @author QZero
 */
public class Datagram {

    public static final String IDENTIFIER_SIGN_IN="SIIN";
    public static final String IDENTIFIER_REPORT="REPO";
    public static final String IDENTIFIER_SEND_MESSAGE ="MESG";
    public static final String IDENTIFIER_GET_MESSAGE_INDEX="MEGI";
    public static final String IDENTIFIER_GET_MESSAGE_DETAIL="MEGD";
    public static final String IDENTIFIER_DELETE_MESSAGE="MEDE";
    public static final String IDENTIFIER_GET_USER_INFO="USIF";
    public static final String IDENTIFIER_GET_USERS_INDEX="USID";
    public static final String IDENTIFIER_UPDATE_USER_INFO="IFUP";


    public static final String IDENTIFIER_RETURN_MESSAGE_INDEX="MERI";
    public static final String IDENTIFIER_RETURN_MESSAGE_DETAIL="MERD";
    public static final String IDENTIFIER_RETURN_USER_INFO="USRF";
    public static final String IDENTIFIER_RETURN_USERS_INDEX="USRI";


    /**
     * 当前协议版本
     */
    public static final int CURRENT_VER_CODE=1;

    /**
     * 标识符，标识该数据包的操作 必须为4个字符
     */
    private String identifier;

    /**
     * 版本号
     */
    private int verCode=CURRENT_VER_CODE;

    /**
     * 数据包时间戳
     */
    private long time;

    /**
     * 不定长参数
     */
    private Map<String,byte[]> params;

    public Datagram() {
    }

    public Datagram(String identifier, int verCode, long time, Map<String, byte[]> params) {
        this.identifier = identifier;
        this.verCode = verCode;
        this.time = time;
        this.params = params;
    }

    public Datagram(String identifier, Map<String, byte[]> params) {
        this.identifier = identifier;
        this.params = params;
        this.verCode=CURRENT_VER_CODE;
        this.time=System.currentTimeMillis();

        //FIXME 这一堆今后肯定得改的
        if(params==null)
            this.params=new HashMap<>();
        if(this.params.isEmpty())
            this.params.put("a","a".getBytes());
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getVerCode() {
        return verCode;
    }

    public void setVerCode(int verCode) {
        this.verCode = verCode;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Map<String, byte[]> getParams() {
        return params;
    }

    public void setParams(Map<String, byte[]> params) {
        this.params = params;
    }

    /**
     * 获取参数 把所有参数值视为字符串
     * @return 参数
     */
    public Map<String,String> getParamsAsString(){
        Map<String,String> result=new HashMap<>();
        Set<String> keys=params.keySet();
        for(String key:keys){
            result.put(key,new String(params.get(key)));
        }
        return result;
    }

    @Override
    public String toString() {
        return "Datagram{" +
                "identifier='" + identifier + '\'' +
                ", verCode=" + verCode +
                ", time=" + time +
                ", params=" + getParamsAsString() +
                '}';
    }
}
