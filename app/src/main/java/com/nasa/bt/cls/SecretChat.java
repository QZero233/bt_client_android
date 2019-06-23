package com.nasa.bt.cls;

import com.nasa.bt.annotations.ClassVerCode;

import java.io.Serializable;

@ClassVerCode(1)
public class SecretChat implements Serializable {

    private String sessionId;
    private String srcUid;
    private String dstUid;
    private String keyHash;
    private int status=STATUS_RUNNING;

    public static final int STATUS_RUNNING=0;
    public static final int STATUS_CLOSED=1;

    public SecretChat() {
    }

    public SecretChat(String sessionId, String srcUid, String dstUid, String keyHash) {
        this.sessionId = sessionId;
        this.srcUid = srcUid;
        this.dstUid = dstUid;
        this.keyHash = keyHash;
    }

    public SecretChat(String sessionId, String srcUid, String dstUid, String keyHash, int status) {
        this.sessionId = sessionId;
        this.srcUid = srcUid;
        this.dstUid = dstUid;
        this.keyHash = keyHash;
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSrcUid() {
        return srcUid;
    }

    public void setSrcUid(String srcUid) {
        this.srcUid = srcUid;
    }

    public String getDstUid() {
        return dstUid;
    }

    public void setDstUid(String dstUid) {
        this.dstUid = dstUid;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SecretChat{" +
                "sessionId='" + sessionId + '\'' +
                ", srcUid='" + srcUid + '\'' +
                ", dstUid='" + dstUid + '\'' +
                ", keyHash='" + keyHash + '\'' +
                '}';
    }
}
