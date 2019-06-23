package com.nasa.bt;

import android.app.Application;

import com.nasa.bt.log.AppLogConfigurator;

public class BugTelegramApplication extends Application {

    public boolean threadRunning=false;


    @Override
    public void onCreate() {
        super.onCreate();
        AppLogConfigurator.configure();
    }

    
}
