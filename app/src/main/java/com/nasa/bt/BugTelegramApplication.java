package com.nasa.bt;

import android.app.Application;
import android.content.Intent;

import com.nasa.bt.ca.CAUtils;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.crypt.AppKeyStore;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.loop.DatagramListener;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;

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
        CAUtils.initContext(this);

        startService(new Intent(this,MessageLoopService.class));
        MessageLoopUtils.registerListener("APPLICATION_RECONNECTION", SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT, 1000, new DatagramListener() {
            @Override
            public void onDatagramReach(Datagram datagram) {
                startService(new Intent(getApplicationContext(),MessageLoopService.class));
            }
        });
    }

}
