package com.nasa.bt.loop;

import android.util.Log;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class LoopResource {

    public static final String INBOX_IDENTIFIER_DISCONNECTED="DISC";
    public static final String INBOX_IDENTIFIER_RECONNECT="RECO";

    private static final Logger log= AppLogConfigurator.getLogger();

    public static List<Datagram> unsent=new ArrayList<>();

    public static void sendDatagram(final Datagram datagram){
        new Thread(){
            @Override
            public void run() {
                super.run();
                synchronized (unsent){
                    if(!MessageLoopService.instance.sendDatagram(datagram)){
                        unsent.add(datagram);
                        log.debug("数据包发送失败 具体内容"+datagram);
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
