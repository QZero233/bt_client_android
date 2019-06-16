package com.nasa.bt.loop;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.LoginInfo;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.socket.SocketIOHelper;
import com.nasa.bt.utils.LocalSettingsUtils;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MessageLoopService extends Service implements Runnable {

    public static final String SERVER_IP_DEFAULT = "208.167.242.129";//208.167.242.129
    private static final int SERVER_PORT = 8848;
    private Socket socket;
    private SocketIOHelper helper;

    public static MessageLoopService instance = null;

    public boolean needReConnect = false;

    private ProcessorHandlers handlers;

    private Handler reconnectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            needReConnect = true;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        handlers = new ProcessorHandlers(this);
        handlers.addDefaultIntents();

        new Thread(this).start();
        new Thread() {
            @Override
            public void run() {
                super.run();

                while (true) {
                    try {
                        Thread.sleep(5000);
                        if (needReConnect) {
                            Log.e("NASA", "开始重连");
                            reConnect();
                            needReConnect = false;
                        }
                    } catch (Exception e) {

                    }
                }

            }
        }.start();

        MessageIntent reconnectIntent = new MessageIntent("SERVICE_RECONNECT", LoopResource.INBOX_IDENTIFIER_RECONNECT, reconnectHandler, 0, 0);

        MessageLoop.addIntent(reconnectIntent);
    }

    public synchronized void reConnect() {
        try {
            socket.close();
        }catch (Exception e){

        }

        new Thread(this).start();
    }

    public boolean sendDatagram(Datagram datagram) {
        if (socket.isClosed() || !socket.isConnected())
            return false;

        try {
            return helper.writeOs(datagram);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean doAuth() {
        LoginInfo info = LoopResource.loginInfo;
        if (info == null) {
            info = new LoginInfo();

            String name = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_NAME);
            String code = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_CODE_HASH);

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(code)) {
                info.name = name;
                info.codeHash = code;
            } else {
                return false;
            }

        }

        Map<String, byte[]> loginParam = new HashMap<>();

        loginParam.put("username", info.name.getBytes());
        loginParam.put("code_hash", info.codeHash.getBytes());

        Datagram loginDatagram = new Datagram(Datagram.IDENTIFIER_SIGN_IN, loginParam);
        return helper.writeOs(loginDatagram);
    }

    @Override
    public void run() {
        try {
            String ip = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_SERVER_IP);
            if (TextUtils.isEmpty(ip))
                ip = SERVER_IP_DEFAULT;

            socket = new Socket(ip, SERVER_PORT);
            helper = new SocketIOHelper(socket.getInputStream(), socket.getOutputStream());

            while(true){
                //接受对方传来的公钥
                Datagram datagram=helper.readIs();
                if(datagram.getIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_NONE))
                    break;
            }

            KeyUtils keyUtils=KeyUtils.getInstance();
            helper.setPrivateKey(keyUtils.getPri());
            while(!helper.sendPublicKey(keyUtils.getPub())){
                Log.e("NASA","交换公钥失败，再次尝试");
                Thread.sleep(1000);
            }

            Log.e("NASA", "连接完成，开始进行身份验证");
            if (!doAuth()) {
                Log.e("NASA", "身份验证失败，继续准备重连");
                needReConnect = true;
                return;
            }

            //处理未发出的数据包
            LoopResource.sendUnsent();

            while (true) {
                //开始循环监听
                Datagram datagram = helper.readIs();
                if (datagram == null)
                    throw new Exception();

                MessageLoop.processDatagram(datagram);
            }

        } catch (Exception e) {
            Log.e("NASA", "正在尝试断线重连 " + e.getMessage());
            needReConnect = true;
        }

    }
}


class ClientThread extends Thread{

    private Context context;

    @Override
    public void run() {
        super.run();


    }
}