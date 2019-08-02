package com.nasa.bt.data.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "trustedPublicKey")
public class TrustedCAPublicKeyEntity {

    @DatabaseField(id = true)
    private String publicKeyHash;
    @DatabaseField
    private String publicKey;

    public TrustedCAPublicKeyEntity() {
    }

    public TrustedCAPublicKeyEntity(String publicKeyHash, String publicKey) {
        this.publicKeyHash = publicKeyHash;
        this.publicKey = publicKey;
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
        return "TrustedCAPublicKeyEntity{" +
                "publicKeyHash='" + publicKeyHash + '\'' +
                ", publicKey='" + publicKey + '\'' +
                '}';
    }
}
