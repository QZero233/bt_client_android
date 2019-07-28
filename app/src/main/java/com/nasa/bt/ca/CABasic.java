package com.nasa.bt.ca;

public class CABasic {

    /**
     * 服务器IP
     */
    private String serverIp;
    /**
     * 服务器公钥
     * 取hash以十六进制编码
     */
    private String serverPubKeyHashInHex;

    /**
     * 到期时间
     */
    private long endTime;
    /**
     * 签发单位的公钥hash，十六进制编码
     */
    private String signPubKeyHashInHex;

    public CABasic(String serverIp, String serverPubKeyHashInHex, long endTime, String signPubKeyHashInHex) {
        this.serverIp = serverIp;
        this.serverPubKeyHashInHex = serverPubKeyHashInHex;
        this.endTime = endTime;
        this.signPubKeyHashInHex = signPubKeyHashInHex;
    }

    public CABasic() {
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getServerPubKeyHashInHex() {
        return serverPubKeyHashInHex;
    }

    public void setServerPubKeyHashInHex(String serverPubKeyHashInHex) {
        this.serverPubKeyHashInHex = serverPubKeyHashInHex;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getSignPubKeyHashInHex() {
        return signPubKeyHashInHex;
    }

    public void setSignPubKeyHashInHex(String signPubKeyHashInHex) {
        this.signPubKeyHashInHex = signPubKeyHashInHex;
    }

    @Override
    public String toString() {
        return "CABasic{" +
                "serverIp='" + serverIp + '\'' +
                ", serverPubKeyHashInHex='" + serverPubKeyHashInHex + '\'' +
                ", endTime=" + endTime +
                ", signPubKeyHashInHex='" + signPubKeyHashInHex + '\'' +
                '}';
    }
}
