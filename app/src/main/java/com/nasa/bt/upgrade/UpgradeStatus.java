package com.nasa.bt.upgrade;

public class UpgradeStatus {

    private int newestVerCode;
    private String newestName;
    private String upgradeLog;
    private String downloadUrl;

    public UpgradeStatus(int newestVerCode, String newestName, String upgradeLog, String downloadUrl) {
        this.newestVerCode = newestVerCode;
        this.newestName = newestName;
        this.upgradeLog = upgradeLog;
        this.downloadUrl = downloadUrl;
    }

    public UpgradeStatus() {
    }

    public int getNewestVerCode() {
        return newestVerCode;
    }

    public void setNewestVerCode(int newestVerCode) {
        this.newestVerCode = newestVerCode;
    }

    public String getNewestName() {
        return newestName;
    }

    public void setNewestName(String newestName) {
        this.newestName = newestName;
    }

    public String getUpgradeLog() {
        return upgradeLog;
    }

    public void setUpgradeLog(String upgradeLog) {
        this.upgradeLog = upgradeLog;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @Override
    public String toString() {
        return "UpgradeStatus{" +
                "newestVerCode=" + newestVerCode +
                ", newestName='" + newestName + '\'' +
                ", upgradeLog='" + upgradeLog + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                '}';
    }
}
