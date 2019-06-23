package com.nasa.bt;

import android.app.Application;

public class BugTelegramApplication extends Application {

    public boolean threadRunning=false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    
}
