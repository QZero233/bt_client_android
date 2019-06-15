package com.nasa.bt.cls;

import com.nasa.bt.annotations.ClassVerCode;

import java.io.Serializable;

@ClassVerCode(2)
/**
 * 用户信息类
 * @author QZero
 */
public class UserInfo implements Serializable {

    private String name;
    private String id;

    public UserInfo() {
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

    public UserInfo(String name, String id) {
        this.name = name;
        this.id = id;
    }
}
