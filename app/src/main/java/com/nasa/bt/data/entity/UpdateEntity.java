package com.nasa.bt.data.entity;

import java.util.Objects;

public class UpdateEntity {

    public static final int TYPE_SESSION_DELETE=1;
    public static final int TYPE_SESSION_CREATE=2;
    public static final int TYPE_SESSION_UPDATED=3;

    private String updateId;
    private String srcUid;
    private String dstUid;

    private int type;
    private Long time;
    private String more;

    public UpdateEntity() {
    }

    public UpdateEntity(String updateId, String srcUid, String dstUid, int type, Long time, String more) {
        this.updateId = updateId;
        this.srcUid = srcUid;
        this.dstUid = dstUid;
        this.type = type;
        this.time = time;
        this.more = more;
    }


    public String getUpdateId() {
        return updateId;
    }

    public void setUpdateId(String updateId) {
        this.updateId = updateId;
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


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }


    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }


    public String getMore() {
        return more;
    }

    public void setMore(String more) {
        this.more = more;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateEntity that = (UpdateEntity) o;
        return type == that.type &&
                Objects.equals(updateId, that.updateId) &&
                Objects.equals(srcUid, that.srcUid) &&
                Objects.equals(dstUid, that.dstUid) &&
                Objects.equals(time, that.time) &&
                Objects.equals(more, that.more);
    }

    @Override
    public int hashCode() {
        return Objects.hash(updateId, srcUid, dstUid, type, time, more);
    }

    @Override
    public String toString() {
        return "UpdateEntity{" +
                "updateId='" + updateId + '\'' +
                ", srcUid='" + srcUid + '\'' +
                ", dstUid='" + dstUid + '\'' +
                ", type=" + type +
                ", time=" + time +
                ", more='" + more + '\'' +
                '}';
    }
}
