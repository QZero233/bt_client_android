package com.nasa.bt;

import android.app.Application;

import com.nasa.bt.log.AppLogConfigurator;

public class BugTelegramApplication extends Application {

    private boolean threadRunning=false;

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

    
}
