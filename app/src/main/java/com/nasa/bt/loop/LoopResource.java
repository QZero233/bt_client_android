package com.nasa.bt.loop;

import android.util.Log;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.LoginInfo;

import java.util.ArrayList;
import java.util.List;

public class LoopResource {

    public static final String INBOX_IDENTIFIER_DISCONNECTED="DISC";
    public static final String INBOX_IDENTIFIER_RECONNECT="RECO";

    public static List<Datagram> unsent=new ArrayList<>();

    public static void cleanUnsent(){
        synchronized (unsent){
            unsent.clear();
        }
    }

    public static void sendDatagram(final Datagram datagram){
        new Thread(){
            @Override
            public void run() {
                super.run();
                synchronized (unsent){
                    if(!MessageLoopService.instance.sendDatagram(datagram)){
                        unsent.add(datagram);
                        Log.e("NASA","数据包发送失败 "+datagram);
                        Datagram reconnect=new Datagram(INBOX_IDENTIFIER_RECONNECT,null);
                        MessageLoop.processDatagram(reconnect);
                    }

                }
            }
        }.start();

    }

    public static void sendUnsent(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                synchronized (unsent){
                    for(Datagram datagram:unsent){
                        if(MessageLoopService.instance.sendDatagram(datagram))
                            unsent.remove(datagram);
                    }
                }
            }
        }.start();
    }

}
