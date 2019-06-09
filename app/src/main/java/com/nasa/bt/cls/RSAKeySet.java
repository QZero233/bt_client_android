package com.nasa.bt.cls;

public class RSAKeySet {

    private String pub;
    private String pri;

    public RSAKeySet() {
    }

    public RSAKeySet(String pub, String pri) {
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

    @Override
    public String toString() {
        return "RSAKeySet{" +
                "pub='" + pub + '\'' +
                ", pri='" + pri + '\'' +
                '}';
    }
}
