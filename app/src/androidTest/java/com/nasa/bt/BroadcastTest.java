package com.nasa.bt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.InstrumentationRegistry;
import android.support.v4.content.LocalBroadcastManager;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class BroadcastTest {

    private Context context;
    private static final Logger log= AppLogConfigurator.getLogger();

    @Before
    public void reg() {
        context = InstrumentationRegistry.getTargetContext();
        LocalBroadcastManager localBroadcastManager=LocalBroadcastManager.getInstance(context);

        BroadcastReceiver receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                log.info("接收器1收到广播 "+intent.getSerializableExtra("datagram"));
            }
        };
        IntentFilter intentFilter=new IntentFilter("com.nasa.bt.Datagram.TEST");
        intentFilter.setPriority(100);

        BroadcastReceiver receiver2=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                log.info("接收器2收到广播 "+intent.getSerializableExtra("datagram"));
            }
        };
        IntentFilter intentFilter2=new IntentFilter("com.nasa.bt.Datagram.TEST");
        intentFilter.setPriority(50);

        localBroadcastManager.registerReceiver(receiver,intentFilter);
        localBroadcastManager.registerReceiver(receiver2,intentFilter2);
    }

    @Test
    public void testBroadcast() {
        LocalBroadcastManager localBroadcastManager=LocalBroadcastManager.getInstance(context);

        Intent intent=new Intent("com.nasa.bt.Datagram.TEST");
        intent.putExtra("datagram",new Datagram(Datagram.IDENTIFIER_REFRESH,new ParamBuilder().putParam("test","yes").build()));
        localBroadcastManager.sendBroadcast(intent);
    }

}
