package com.nasa.bt;

import android.app.Application;

import com.nasa.bt.log.AppLogConfigurator;

public class BugTelegramApplication extends Application {

    private boolean threadRunning=false;

    public synchronized void setThreadRunningStatus(boolean isRunning){
        threadRunning=isRunning;
    }

    public synchronized boolean isThreadRunning(){
        return threadRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLogConfigurator.configure();
    }

}
