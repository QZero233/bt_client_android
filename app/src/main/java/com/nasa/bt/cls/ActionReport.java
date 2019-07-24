package com.nasa.bt.cls;

public class ActionReport {
    private String actionStatus;
    private String actionIdentifier;
    private String replyId;
    private String more;

    public static final String STATUS_SUCCESS="1";
    public static final String STATUS_FAILURE="0";

    public ActionReport() {
    }

    public ActionReport(String actionStatus, String actionIdentifier, String replyId, String more) {
        this.actionStatus = actionStatus;
        this.actionIdentifier = actionIdentifier;
        this.replyId = replyId;
        this.more = more;
    }

    public String getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(String actionStatus) {
        this.actionStatus = actionStatus;
    }

    public String getActionIdentifier() {
        return actionIdentifier;
    }

    public void setActionIdentifier(String actionIdentifier) {
        this.actionIdentifier = actionIdentifier;
    }

    public String getReplyId() {
        return replyId;
    }

    public void setReplyId(String replyId) {
        this.replyId = replyId;
    }

    public String getMore() {
        return more;
    }

    public void setMore(String more) {
        this.more = more;
    }

    public boolean getActionStatusInBoolean(){
        return actionStatus.equals(STATUS_SUCCESS);
    }

    @Override
    public String toString() {
        return "ActionReport{" +
                "actionStatus=" + actionStatus +
                ", actionIdentifier='" + actionIdentifier + '\'' +
                ", replyId='" + replyId + '\'' +
                ", more='" + more + '\'' +
                '}';
    }
}
