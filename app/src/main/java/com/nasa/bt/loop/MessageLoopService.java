package com.nasa.bt.loop;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.nasa.bt.BugTelegramApplication;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.socket.ClientHandShakeHelper;
import com.nasa.bt.socket.SocketIOHelper;
import com.nasa.bt.utils.LocalSettingsUtils;

import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MessageLoopService extends Service {

    public static final String SERVER_IP_DEFAULT = "134.175.96.107";//208.167.242.129
    public static final int SERVER_PORT = 8848;

    public static final int STATUS_CONNECTING = 0;
    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_DISCONNECTED = 2;

    private ProcessorHandlers handlers;

    public static MessageLoopService instance = null;

    public ClientThread connection;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        rebind();

        final BugTelegramApplication application = (BugTelegramApplication) getApplication();

        MessageLoopUtils.registerListenerDefault("SERVICE_RECONNECT", SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT, new DatagramListener() {
            @Override
            public void onDatagramReach(Datagram datagram) {
                //线程自然死亡后软重连会无效，只能硬重连
                if (!application.isThreadRunning()) {
                    connection = new ClientThread(MessageLoopService.this);
                    connection.start();
                    application.setThreadRunningStatus(true);
                } else
                    connection.reconnect();
            }
        });

        MessageLoopUtils.registerListenerDefault("SERVICE_DISCONNECT", SendDatagramUtils.INBOX_IDENTIFIER_DISCONNECTED, new DatagramListener() {
            @Override
            public void onDatagramReach(Datagram datagram) {
                connection.stopConnection();
            }
        });

        if (!application.isThreadRunning()) {
            connection = new ClientThread(this);
            connection.start();
            application.setThreadRunningStatus(true);
        }else{
            MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_CONNECTION_STATUS,new ParamBuilder().putParam("status",String.valueOf(STATUS_CONNECTED)));
        }

        return super.onStartCommand(intent, flags, startId);
    }


    public void rebind() {
        handlers = new ProcessorHandlers(this);
        handlers.addDefaultIntents();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connection.stopConnection();
    }

    public boolean sendDatagram(Datagram datagram) {
        return connection.sendDatagram(datagram);
    }

}


class ClientThread extends Thread {

    private String currentIp;

    private MessageLoopService parent;
    private Socket socket;
    private SocketIOHelper helper;
    private boolean running = true;

    private int tryTime = 0;

    private BugTelegramApplication application;

    private static final Logger log = AppLogConfigurator.getLogger();

    public ClientThread(MessageLoopService parent) {
        this.parent = parent;
        application = (BugTelegramApplication) parent.getApplication();

        currentIp = LocalSettingsUtils.read(parent, LocalSettingsUtils.FIELD_SERVER_IP);
        if (TextUtils.isEmpty(currentIp))
            currentIp = MessageLoopService.SERVER_IP_DEFAULT;
    }

    private synchronized void changeConnectionStatus(int newStatus){
        MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_CONNECTION_STATUS,new ParamBuilder().putParam("status",String.valueOf(newStatus)));
    }

    public synchronized void stopConnection() {
        log.debug("收到断线通知，开始手动断线");
        application.setThreadRunningStatus(false);

        running = false;
        try {
            socket.close();
        } catch (Exception e) {

        }
    }

    public synchronized void reconnect() {
        log.debug("收到手动重连通知，开始手动重连");
        tryTime = 0;
        try {
            socket.close();
        } catch (Exception e) {
        }

        parent.rebind();
    }

    @Override
    public void run() {
        super.run();

        Looper.prepare();

        while (running) {
            changeConnectionStatus(MessageLoopService.STATUS_DISCONNECTED);//

            tryTime++;
            if (tryTime >= 5)
                tryTime = 5;

            log.info("因未知原因断线，" + tryTime + "秒后将尝试重连");

            changeConnectionStatus(MessageLoopService.STATUS_CONNECTING);//重连中

            try {
                doProcess();
            }catch (Error e){

            }catch (Exception e){

            }

            try {
                for (int i = 0; i < tryTime; i++) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {

            }
        }

        log.info("监听线程结束（自然死亡）");
    }

    private void doProcess() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(currentIp, MessageLoopService.SERVER_PORT), 10000);
            helper = new SocketIOHelper(socket.getInputStream(), socket.getOutputStream());

            ClientHandShakeHelper handShakeHelper=new ClientHandShakeHelper(parent,helper,currentIp);
            if(!handShakeHelper.doHandShake())
                return;



            log.info("握手完成，开始进行身份验证");
            if (!doAuth()) {
                log.info("身份验证失败（本地原因），准备重连");
                return;
            }

            log.info("身份验证数据包已发送，开始循环监听");
            tryTime = 0;

            changeConnectionStatus(MessageLoopService.STATUS_CONNECTED);//已连接

            while (true) {
                //开始循环监听
                Datagram datagram = helper.readIs();
                if (datagram == null)
                    break;

                MessageLoopUtils.receivedDatagram(datagram);
            }

        } catch (Exception e) {
            log.error("处理数据包时错误", e);
        }
    }

    private boolean doAuth() {
        String name = LocalSettingsUtils.read(parent, LocalSettingsUtils.FIELD_NAME);
        String code = LocalSettingsUtils.read(parent, LocalSettingsUtils.FIELD_CODE_HASH);

        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(code)) {
            return false;
        }

        Map<String, byte[]> loginParam = new HashMap<>();

        loginParam.put("username", name.getBytes());
        loginParam.put("code_hash", code.getBytes());

        Datagram loginDatagram = new Datagram(Datagram.IDENTIFIER_SIGN_IN, loginParam);
        return helper.writeOs(loginDatagram);
    }

    public boolean sendDatagram(Datagram datagram) {
        if (socket.isClosed() || !socket.isConnected())
            return false;

        try {
            return helper.writeOs(datagram);
        } catch (Exception e) {
            log.debug("发送数据包错误", e);
            return false;
        }
    }

}