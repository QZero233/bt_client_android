package com.nasa.bt.cls;

public class RSAKeySet {

    private String name="";
    private String pub;
    private String pri;

    public RSAKeySet() {
    }

    public RSAKeySet(String pub, String pri) {
        this.pub = pub;
        this.pri = pri;
    }

    public RSAKeySet(String name, String pub, String pri) {
        this.name = name;
        this.pub = pub;
        this.pri = pri;
    }

    public String getPub() {
        return pub;
    }

    public void setPub(String pub) {
        this.pub = pub;
    }

    public String getPri() {
        return pri;
    }

    public void setPri(String pri) {
        this.pri = pri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "RSAKeySet{" +
                "name='" + name + '\'' +
                ", pub='" + pub + '\'' +
                ", pri='" + pri + '\'' +
                '}';
    }
}
