package com.nasa.bt.data.entity;

public class SessionEntityForShow {

    private String sessionId;
    private SessionEntity sessionEntity;
    private UserInfoEntity userInfoEntity;
    private int unreadMessageCount;
    private MessageEntity lastUnreadMessage;

    public SessionEntityForShow(String sessionId, SessionEntity sessionEntity, UserInfoEntity userInfoEntity, int unreadMessageCount, MessageEntity lastUnreadMessage) {
        this.sessionId = sessionId;
        this.sessionEntity = sessionEntity;
        this.userInfoEntity = userInfoEntity;
        this.unreadMessageCount = unreadMessageCount;
        this.lastUnreadMessage = lastUnreadMessage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public SessionEntity getSessionEntity() {
        return sessionEntity;
    }

    public void setSessionEntity(SessionEntity sessionEntity) {
        this.sessionEntity = sessionEntity;
    }

    public UserInfoEntity getUserInfoEntity() {
        return userInfoEntity;
    }

    public void setUserInfoEntity(UserInfoEntity userInfoEntity) {
        this.userInfoEntity = userInfoEntity;
    }

    public int getUnreadMessageCount() {
        return unreadMessageCount;
    }

    public void setUnreadMessageCount(int unreadMessageCount) {
        this.unreadMessageCount = unreadMessageCount;
    }

    public MessageEntity getLastUnreadMessage() {
        return lastUnreadMessage;
    }

    public void setLastUnreadMessage(MessageEntity lastUnreadMessage) {
        this.lastUnreadMessage = lastUnreadMessage;
    }

    @Override
    public String toString() {
        return "SessionEntityForShow{" +
                "sessionId='" + sessionId + '\'' +
                ", sessionEntity=" + sessionEntity +
                ", userInfoEntity=" + userInfoEntity +
                ", unreadMessageCount=" + unreadMessageCount +
                ", lastUnreadMessage=" + lastUnreadMessage +
                '}';
    }
}
