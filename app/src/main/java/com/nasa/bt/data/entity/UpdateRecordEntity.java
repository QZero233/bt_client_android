package com.nasa.bt.data.entity;

import java.util.Objects;

public class UpdateRecordEntity {

    private String entityId;
    private int entityType;
    private int entityStatus;
    private long lastEditTime;

    public static final int TYPE_SESSION=0;
    public static final int TYPE_USER=1;

    public static final int STATUS_ALIVE=1;
    public static final int STATUS_DELETED=0;

    public UpdateRecordEntity() {
    }

    public UpdateRecordEntity(String entityId, int entityType, int entityStatus, long lastEditTime) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.entityStatus = entityStatus;
        this.lastEditTime = lastEditTime;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public int getEntityType() {
        return entityType;
    }

    public void setEntityType(int entityType) {
        this.entityType = entityType;
    }

    public long getLastEditTime() {
        return lastEditTime;
    }

    public void setLastEditTime(long lastEditTime) {
        this.lastEditTime = lastEditTime;
    }

    public int getEntityStatus() {
        return entityStatus;
    }

    public void setEntityStatus(int entityStatus) {
        this.entityStatus = entityStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateRecordEntity that = (UpdateRecordEntity) o;
        return entityType == that.entityType &&
                entityStatus == that.entityStatus &&
                lastEditTime == that.lastEditTime &&
                Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, entityType, entityStatus, lastEditTime);
    }

    @Override
    public String toString() {
        return "UpdateRecordEntity{" +
                "entityId='" + entityId + '\'' +
                ", entityType=" + entityType +
                ", entityStatus=" + entityStatus +
                ", lastEditTime=" + lastEditTime +
                '}';
    }

}
