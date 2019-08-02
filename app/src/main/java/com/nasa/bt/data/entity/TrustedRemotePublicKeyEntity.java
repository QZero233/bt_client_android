package com.nasa.bt.data.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.nasa.bt.crypt.SHA256Utils;

@DatabaseTable(tableName = "trustedRemotePublicKey")
public class TrustedRemotePublicKeyEntity {

    @DatabaseField(id = true)
    private String ip;
    @DatabaseField
    private String publicKey;
    @DatabaseField
    private String publicKeyHash;

    public TrustedRemotePublicKeyEntity() {

    }

    public TrustedRemotePublicKeyEntity(String ip, String publicKey) {
        this.ip = ip;
        this.publicKey = publicKey;
        this.publicKeyHash= SHA256Utils.getSHA256InHex(publicKey);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPublicKeyHash() {
        return publicKeyHash;
    }

    public void setPublicKeyHash(String publicKeyHash) {
        this.publicKeyHash = publicKeyHash;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String toString() {
        return "TrustedRemotePublicKeyEntity{" +
                "ip='" + ip + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", publicKeyHash='" + publicKeyHash + '\'' +
                '}';
    }
}
