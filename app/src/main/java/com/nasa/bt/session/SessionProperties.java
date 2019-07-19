package com.nasa.bt.session;

public class SessionProperties {

    private String sessionName;
    private int sessionTextColor;
    private String chatTitleEndWith;
    private String mainNameEndWith;

    public SessionProperties(String sessionName, int sessionTextColor, String chatTitleEndWith, String mainNameEndWith) {
        this.sessionName = sessionName;
        this.sessionTextColor = sessionTextColor;
        this.chatTitleEndWith = chatTitleEndWith;
        this.mainNameEndWith = mainNameEndWith;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public int getSessionTextColor() {
        return sessionTextColor;
    }

    public void setSessionTextColor(int sessionTextColor) {
        this.sessionTextColor = sessionTextColor;
    }

    public String getChatTitleEndWith() {
        return chatTitleEndWith;
    }

    public void setChatTitleEndWith(String chatTitleEndWith) {
        this.chatTitleEndWith = chatTitleEndWith;
    }

    public String getMainNameEndWith() {
        return mainNameEndWith;
    }

    public void setMainNameEndWith(String mainNameEndWith) {
        this.mainNameEndWith = mainNameEndWith;
    }

    @Override
    public String toString() {
        return "SessionProperties{" +
                "sessionName='" + sessionName + '\'' +
                ", sessionTextColor=" + sessionTextColor +
                ", chatTitleEndWith='" + chatTitleEndWith + '\'' +
                ", mainNameEndWith='" + mainNameEndWith + '\'' +
                '}';
    }
}
