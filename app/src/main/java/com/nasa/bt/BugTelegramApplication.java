package com.nasa.bt;

import android.app.Application;

import com.nasa.bt.crypt.AppKeyStore;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.loop.MessageLoopUtils;

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
        MessageLoopUtils.initContext(this);
        AppKeyStore.getInstance().initKeyStore(this);
        KeyUtils.initContext(this);
    }

}
