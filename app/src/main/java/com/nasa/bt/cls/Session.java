package com.nasa.bt.cls;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.annotations.ClassVerCode;
import com.nasa.bt.annotations.MainKey;

import java.io.Serializable;
import java.util.Map;

@ClassVerCode(1)
public class Session implements Serializable {

    public static final int TYPE_NORMAL=0;
    public static final int TYPE_SECRET_CHAT=1;

    @MainKey(true)
    private String sessionId;
    private int sessionType;

    private String uidSrc;
    private String uidDst;

    private String params;

    private long lastTime;
    private String lastMessage;


    public Session() {
    }

    public Session(String sessionId, int sessionType, String uidSrc, String uidDst, Map<String,String> params, long lastTime, String lastMessage) {
        this.sessionId = sessionId;
        this.sessionType = sessionType;
        this.uidSrc = uidSrc;
        this.uidDst = uidDst;
        this.params = JSON.toJSONString(params);
        this.lastTime = lastTime;
        this.lastMessage = lastMessage;
    }

    public Session(String sessionId, int sessionType, String uidSrc, String uidDst, String params, long lastTime, String lastMessage) {
        this.sessionId = sessionId;
        this.sessionType = sessionType;
        this.uidSrc = uidSrc;
        this.uidDst = uidDst;
        this.params = params;
        this.lastTime = lastTime;
        this.lastMessage = lastMessage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getSessionType() {
        return sessionType;
    }

    public void setSessionType(int sessionType) {
        this.sessionType = sessionType;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Map<String,String> getParamsInMap(){
        return (Map<String, String>) JSON.parse(params);
    }

    public void setParamsInMap(Map<String,String> params){
        this.params=JSON.toJSONString(params);
    }

    public String getUidSrc() {
        return uidSrc;
    }

    public void setUidSrc(String uidSrc) {
        this.uidSrc = uidSrc;
    }

    public String getUidDst() {
        return uidDst;
    }

    public void setUidDst(String uidDst) {
        this.uidDst = uidDst;
    }

    public String getIdOfOther(String uid){
        if(uid==null)
            return null;
        if(uid.equals(uidSrc))
            return uidDst;
        return uidSrc;
    }

    public boolean checkInSession(String uid){
        if(uid==null)
            return false;
        if(!uid.equals(uidSrc) && !uid.equals(uidDst))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", sessionType=" + sessionType +
                ", lastTime=" + lastTime +
                ", lastMessage='" + lastMessage + '\'' +
                ", params='" + params + '\'' +
                ", uidSrc='" + uidSrc + '\'' +
                ", uidDst='" + uidDst + '\'' +
                '}';
    }
}
