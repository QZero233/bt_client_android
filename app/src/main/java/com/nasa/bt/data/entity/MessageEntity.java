package com.nasa.bt.data.entity;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "message")
public class MessageEntity {
    @DatabaseField(id = true)
    private String msgId;
    @DatabaseField
    private String srcUid;
    @DatabaseField
    private String dstUid;
    @DatabaseField
    private String sessionId;
    @DatabaseField
    private String content;
    @DatabaseField
    private long time;
    @DatabaseField
    private int status=STATUS_SENDING;

    public static final int STATUS_SENDING=0;
    public static final int STATUS_FAILED=-1;
    public static final int STATUS_UNREAD=1;
    public static final int STATUS_READ=2;

    public MessageEntity() {
    }

    public MessageEntity(String msgId, String srcUid, String dstUid, String sessionId, String content, long time) {
        this.msgId = msgId;
        this.srcUid = srcUid;
        this.dstUid = dstUid;
        this.sessionId = sessionId;
        this.content = content;
        this.time = time;
    }

    public MessageEntity(String msgId, String srcUid, String dstUid, String sessionId, String content, long time, int status) {
        this.msgId = msgId;
        this.srcUid = srcUid;
        this.dstUid = dstUid;
        this.sessionId = sessionId;
        this.content = content;
        this.time = time;
        this.status = status;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "msgId='" + msgId + '\'' +
                ", srcUid='" + srcUid + '\'' +
                ", dstUid='" + dstUid + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", content='" + content + '\'' +
                ", time=" + time +
                ", status=" + status +
                '}';
    }
}
