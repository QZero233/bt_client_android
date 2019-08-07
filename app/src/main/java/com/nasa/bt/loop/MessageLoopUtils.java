package com.nasa.bt.loop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageLoopUtils {

    private static Map<String, BroadcastReceiver> registered = new HashMap<>();
    private static Context context;
    //private static LocalBroadcastManager localBroadcastManager;

    public static final String ACTION_BEGIN_WITH = "com.nasa.bt.Datagram.";

    public static final int PRIORITY_DEFAULT = 10;

    public static final int PRIORITY_NORMAL = 1;

    public static void initContext(Context c) {
        context = c;
        //localBroadcastManager = LocalBroadcastManager.getInstance(c);
    }

    public static void registerListener(String id, String identifier, int priority, final DatagramListener listener) {
        if (listener == null)
            return;

        unregisterListener(id);

        IntentFilter intentFilter = new IntentFilter(ACTION_BEGIN_WITH + identifier);
        intentFilter.setPriority(priority);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Datagram datagram = (Datagram) intent.getSerializableExtra("datagram");
                listener.onDatagramReach(datagram);
            }
        };

        //TODO 安全性问题以后再说
        context.registerReceiver(receiver,intentFilter);
        //localBroadcastManager.registerReceiver(receiver, intentFilter);
        registered.put(id,receiver);
    }

    public static void registerListenerNormal(String id, String identifier, final DatagramListener listener) {
        registerListener(id, identifier, PRIORITY_NORMAL, listener);
    }

    public static void registerListenerDefault(String id, String identifier, final DatagramListener listener) {
        registerListener(id, identifier, PRIORITY_DEFAULT, listener);
    }

    public static void registerActionReportListener(String id, final String identifier, int priority, final ActionReportListener listener) {
        DatagramListener newListener = new DatagramListener() {
            @Override
            public void onDatagramReach(Datagram datagram) {
                ActionReport actionReport = JSON.parseObject(datagram.getParamsAsString().get("action_report"), ActionReport.class);
                if (!actionReport.getActionIdentifier().equalsIgnoreCase(identifier))
                    return;
                listener.onActionReportReach(actionReport);
            }
        };

        registerListener(id, Datagram.IDENTIFIER_REPORT, priority, newListener);
    }

    public static void registerActionReportListenerNormal(String id, final String identifier, final ActionReportListener listener) {
        registerActionReportListener(id,identifier,PRIORITY_NORMAL,listener);
    }

    public static void registerActionReportListenerDefault(String id, final String identifier, final ActionReportListener listener) {
        registerActionReportListener(id,identifier,PRIORITY_DEFAULT,listener);
    }

    public static void registerSpecifiedTimesListener(final String id, String identifier, final int times, final DatagramListener listener){
        DatagramListener newListener=new DatagramListener() {
            int count=0;

            @Override
            public void onDatagramReach(Datagram datagram) {
                count++;
                if(count>times){
                    unregisterListener(id);
                    return;
                }

                if(listener!=null)
                    listener.onDatagramReach(datagram);
            }
        };

        registerListenerNormal(id,identifier,newListener);
    }

    public static void registerSpecifiedTimesActionReportListener(final String id, String identifier, final int times, final ActionReportListener listener){
        ActionReportListener newListener=new ActionReportListener() {
            int count=0;

            @Override
            public void onActionReportReach(ActionReport actionReport) {
                count++;
                if(count>times){
                    unregisterListener(id);
                    return;
                }

                if(listener!=null)
                    listener.onActionReportReach(actionReport);
            }
        };

        registerActionReportListenerNormal(id,identifier,newListener);
    }

    public static void unregisterListener(String id) {
        if (registered.get(id) != null) {
            context.unregisterReceiver(registered.get(id));
            registered.remove(id);
        }
    }

    public static void receivedDatagram(Datagram datagram) {
        if (datagram == null)
            return;
        Intent intent = new Intent(ACTION_BEGIN_WITH + datagram.getIdentifier());
        intent.putExtra("datagram", datagram);
        context.sendOrderedBroadcast(intent,null);
    }

    public static void sendLocalDatagram(String identifier) {
        sendLocalDatagram(identifier,new ParamBuilder());
    }

    public static void sendLocalDatagram(String identifier, ParamBuilder param) {
        Intent intent = new Intent(ACTION_BEGIN_WITH + identifier);
        intent.putExtra("datagram",new Datagram(identifier,param.build()));
        context.sendOrderedBroadcast(intent,null);
    }
}
