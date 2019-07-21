package com.nasa.bt.loop;

import com.nasa.bt.cls.ActionReport;

public interface ActionReportListener {
    void onActionReportReach(ActionReport actionReport);
}
