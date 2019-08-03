package com.nasa.bt.cls;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 数据包类
 * @author QZero
 */
public class Datagram implements Serializable {

    public static final String IDENTIFIER_SIGN_IN="SIIN";
    public static final String IDENTIFIER_REPORT="REPO";
    public static final String IDENTIFIER_SEND_MESSAGE ="MESG";
    public static final String IDENTIFIER_MESSAGE_INDEX="MEGI";
    public static final String IDENTIFIER_MESSAGE_DETAIL="MEGD";
    public static final String IDENTIFIER_DELETE_MESSAGE="MEDE";
    public static final String IDENTIFIER_USER_INFO="USIF";

    public static final String IDENTIFIER_MARK_READ="MKRD";

    public static final String IDENTIFIER_NONE="NONE";

    public static final String IDENTIFIER_CREATE_SESSION="CRSS";
    public static final String IDENTIFIER_SESSIONS_INDEX="GISS";
    public static final String IDENTIFIER_SESSION_DETAIL="GDSS";
    public static final String IDENTIFIER_DELETE_SESSION="DESS";
    public static final String IDENTIFIER_UPDATE_SESSION="UPSS";

    public static final String IDENTIFIER_UPDATE_RECORD="UPRD";

    public static final String IDENTIFIER_REFRESH="REFE";
    public static final String IDENTIFIER_SYNC="SYNC";

    public static final String IDENTIFIER_UPGRADE_VER_CODE="VERC";
    public static final String IDENTIFIER_UPGRADE_DETAIL="UGDE";

    public static final String IDENTIFIER_USER_INFO_MINE="UIMI";

    public static final String HANDSHAKE_FEEDBACK_SUCCESS="SUCCESS";
    public static final String HANDSHAKE_FEEDBACK_CA_WRONG="CA_WRONG";

    public static final int ID_LENGTH=36;

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


        if(params==null)
            this.params=new HashMap<>();
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
