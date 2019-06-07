package com.nasa.bt.cls;

/**
 * 用户信息类
 * @author QZero
 */
public class UserInfo {

    private String name;
    private String id;
    private String key;

    public UserInfo() {
    }

    public UserInfo(String name, String id, String key) {
        this.name = name;
        this.id = id;
        this.key = key;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
