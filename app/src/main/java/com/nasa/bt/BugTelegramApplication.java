package com.nasa.bt;

import android.app.Application;

import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Level;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class BugTelegramApplication extends Application {

    public boolean threadRunning=false;


    @Override
    public void onCreate() {
        super.onCreate();

        AppLogConfigurator.configure();
    }

    
}
