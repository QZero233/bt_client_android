package com.nasa.bt.loop;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.nasa.bt.BugTelegramApplication;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.socket.SocketIOHelper;
import com.nasa.bt.utils.LocalSettingsUtils;

import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MessageLoopService extends Service {

    public static final String SERVER_IP_DEFAULT = "208.167.242.129";//208.167.242.129
    public static final int SERVER_PORT = 8848;

    private ProcessorHandlers handlers;

    public static MessageLoopService instance = null;

    private Handler reconnectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            reConnect();
        }
    };

    private Handler disconnectHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            disconnect();
        }
    };

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

        BugTelegramApplication application= (BugTelegramApplication) getApplication();
        if(!application.getThreadStatus()){
            connection = new ClientThread(this);
            connection.start();
            application.setThreadStatus(true);
        }

        MessageIntent reconnectIntent = new MessageIntent("SERVICE_RECONNECT", LoopResource.INBOX_IDENTIFIER_RECONNECT, reconnectHandler, 0, 0);
        MessageIntent disconnectIntent=new MessageIntent("SERVICE_DISCONNECT",LoopResource.INBOX_IDENTIFIER_DISCONNECTED,disconnectHandler,0,0);

        MessageLoop.addIntent(reconnectIntent);
        MessageLoop.addIntent(disconnectIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    private void reConnect() {
        connection.reconnect();
    }

    private void disconnect(){
        connection.stopConnection();
    }

    public void rebind(){
        handlers=new ProcessorHandlers(this);
        handlers.addDefaultIntents();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connection.stopConnection();
    }

    public boolean sendDatagram(Datagram datagram){
        return connection.sendDatagram(datagram);
    }
}


class ClientThread extends Thread {

    private MessageLoopService parent;
    private Socket socket;
    private SocketIOHelper helper;
    private boolean running=true;

    private static final Logger log= AppLogConfigurator.getLogger();


    public ClientThread(MessageLoopService parent) {
        this.parent = parent;
    }

    public synchronized void stopConnection(){
        log.debug("收到断线通知，开始手动断线");
        BugTelegramApplication application= (BugTelegramApplication) parent.getApplication();
        application.setThreadStatus(false);

        running=false;
        try{
            socket.close();
        }catch (Exception e){

        }
    }

    public synchronized void reconnect(){
        log.debug("收到手动重连通知，开始手动重连");
        try {
            socket.close();
        }catch (Exception e){
        }

        parent.rebind();
    }

    @Override
    public void run() {
        super.run();

        Looper.prepare();

        while(running){
            doProcess();
            log.info("因未知原因断线，5秒后将尝试重连");
            try {
                Thread.sleep(5000);
            }catch (Exception e){

            }
        }

        log.info("监听线程结束（自然死亡）");
    }

    private void doProcess(){
        try {
            String ip = LocalSettingsUtils.read(parent, LocalSettingsUtils.FIELD_SERVER_IP);
            if (TextUtils.isEmpty(ip))
                ip = MessageLoopService.SERVER_IP_DEFAULT;

            socket = new Socket();
            socket.connect(new InetSocketAddress(ip,MessageLoopService.SERVER_PORT),10000);
            helper = new SocketIOHelper(socket.getInputStream(), socket.getOutputStream());

            while (true) {
                //接受对方传来的公钥
                Datagram datagram = helper.readIs();
                if (datagram.getIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_NONE)){
                    log.info("已收到服务器公钥");
                    break;
                }

            }

            KeyUtils.initContext(parent);
            KeyUtils keyUtils = KeyUtils.getInstance();
            helper.setPrivateKey(keyUtils.getPri());
            while (!helper.sendPublicKey(keyUtils.getPub())) {
                log.info("向服务器发送公钥失败，1秒后将再次尝试");
                Thread.sleep(1000);
            }

            log.info("公钥交换完成，开始进行身份验证");
            if (!doAuth()) {
                log.info("身份验证失败（本地原因），准备重连");
                return;
            }

            log.info("身份验证数据包已发送，开始循环监听");

            while (true) {
                //开始循环监听
                Datagram datagram = helper.readIs();
                if (datagram == null)
                    break;

                MessageLoop.processDatagram(datagram);
            }

        } catch (Exception e) {
            log.error("处理数据包时错误",e);
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
            log.debug("发送数据包错误",e);
            return false;
        }
    }

}