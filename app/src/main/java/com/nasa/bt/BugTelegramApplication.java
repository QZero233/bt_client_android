package com.nasa.bt;

import android.app.Application;

import com.nasa.bt.log.AppLogConfigurator;

public class BugTelegramApplication extends Application {

    private boolean threadRunning=false;
    private int connectionStatus;

    public synchronized void setThreadStatus(boolean status){
        threadRunning=status;
    }

    public synchronized boolean getThreadStatus(){
        return threadRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLogConfigurator.configure();
    }

    public synchronized int getConnectionStatus() {
        return connectionStatus;
    }

    public synchronized void setConnectionStatus(int status){
        connectionStatus=status;
    }
}
