package com.nasa.bt.data.entity;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;


/**
 * 用户信息类
 * @author QZero
 */
@DatabaseTable(tableName = "userInfo")
public class UserInfoEntity implements Serializable {

    @DatabaseField(id = true)
    private String id;
    @DatabaseField
    private String name;


    public UserInfoEntity() {
    }


    public UserInfoEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "UserInfoEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
