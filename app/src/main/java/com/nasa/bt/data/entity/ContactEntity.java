package com.nasa.bt.data.entity;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * 本地联系人类
 */
@DatabaseTable(tableName = "contact")
public class ContactEntity {

    @DatabaseField(id=true)
    private String dstUid;

    public ContactEntity() {
    }

    public ContactEntity(String dstUid) {
        this.dstUid = dstUid;
    }

    public String getDstUid() {
        return dstUid;
    }

    public void setDstUid(String dstUid) {
        this.dstUid = dstUid;
    }

    @Override
    public String toString() {
        return "ContactEntity{" +
                "dstUid='" + dstUid + '\'' +
                '}';
    }
}
