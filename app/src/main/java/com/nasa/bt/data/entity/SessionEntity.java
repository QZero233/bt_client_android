package com.nasa.bt.data.entity;

import com.alibaba.fastjson.JSON;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


import java.io.Serializable;
import java.util.Map;

@DatabaseTable(tableName = "session")
public class SessionEntity implements Serializable {

    public static final int TYPE_NORMAL=0;
    public static final int TYPE_SECRET_CHAT=1;

    @DatabaseField(id = true)
    private String sessionId;
    @DatabaseField
    private int sessionType;

    @DatabaseField
    private String srcUid;
    @DatabaseField
    private String dstUid;

    @DatabaseField
    private String params;

    @DatabaseField
    private long lastTime;
    @DatabaseField
    private String lastMessage;

    @DatabaseField
    private boolean disabled=false;


    public SessionEntity() {
    }

    public SessionEntity(String sessionId, int sessionType, String uidSrc, String uidDst, Map<String,String> params, long lastTime, String lastMessage) {
        this.sessionId = sessionId;
        this.sessionType = sessionType;
        this.srcUid = uidSrc;
        this.dstUid = uidDst;
        this.params = JSON.toJSONString(params);
        this.lastTime = lastTime;
        this.lastMessage = lastMessage;
    }

    public SessionEntity(String sessionId, int sessionType, String uidSrc, String uidDst, String params, long lastTime, String lastMessage) {
        this.sessionId = sessionId;
        this.sessionType = sessionType;
        this.srcUid = uidSrc;
        this.dstUid = uidDst;
        this.params = params;
        this.lastTime = lastTime;
        this.lastMessage = lastMessage;
    }

    public SessionEntity(String sessionId, int sessionType, String srcUid, String dstUid, String params, long lastTime, String lastMessage, boolean disabled) {
        this.sessionId = sessionId;
        this.sessionType = sessionType;
        this.srcUid = srcUid;
        this.dstUid = dstUid;
        this.params = params;
        this.lastTime = lastTime;
        this.lastMessage = lastMessage;
        this.disabled = disabled;
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

    public String getSrcUid() {
        return srcUid;
    }

    public void setSrcUid(String uidSrc) {
        this.srcUid = uidSrc;
    }

    public String getDstUid() {
        return dstUid;
    }

    public void setDstUid(String uidDst) {
        this.dstUid = uidDst;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getIdOfOther(String uid){
        if(uid==null)
            return null;
        if(uid.equals(srcUid))
            return dstUid;
        return srcUid;
    }

    public boolean checkInSession(String uid){
        if(uid==null)
            return false;
        if(!uid.equals(srcUid) && !uid.equals(dstUid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SessionEntity{" +
                "sessionId='" + sessionId + '\'' +
                ", sessionType=" + sessionType +
                ", srcUid='" + srcUid + '\'' +
                ", dstUid='" + dstUid + '\'' +
                ", params='" + params + '\'' +
                ", lastTime=" + lastTime +
                ", lastMessage='" + lastMessage + '\'' +
                ", disabled=" + disabled +
                '}';
    }
}
