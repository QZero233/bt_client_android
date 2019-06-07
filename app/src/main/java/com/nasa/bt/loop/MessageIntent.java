package com.nasa.bt.loop;

import android.os.Handler;

public class MessageIntent {

    /**
     * 该监听器的id
     */
    private String id;

    /**
     * 监听的标识符
     */
    private String identifier;

    /**
     * 处理的handler
     */
    private Handler proHandler;

    /**
     * 响应码
     */
    private int responseCode;

    /**
     * 优先级
     */
    private int level;

    public MessageIntent(String id, String identifier, Handler proHandler, int responseCode, int level) {
        this.id = id;
        this.identifier = identifier;
        this.proHandler = proHandler;
        this.responseCode = responseCode;
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Handler getProHandler() {
        return proHandler;
    }

    public void setProHandler(Handler proHandler) {
        this.proHandler = proHandler;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    @Override
    public String toString() {
        return "MessageIntent{" +
                "id='" + id + '\'' +
                ", identifier='" + identifier + '\'' +
                ", responseCode=" + responseCode +
                ", level=" + level +
                '}';
    }
}
