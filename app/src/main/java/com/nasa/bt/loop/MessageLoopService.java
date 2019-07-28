package com.nasa.bt.loop;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.nasa.bt.BugTelegramApplication;
import com.nasa.bt.SettingsActivity;
import com.nasa.bt.ca.CAObject;
import com.nasa.bt.ca.CAUtils;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
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
            }catch (OutOfMemoryError e){

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

    private String getNeed(){
        String need=SocketIOHelper.NEED_PUB_KEY+",";
        if(LocalSettingsUtils.readBoolean(parent,LocalSettingsUtils.FIELD_FORCE_CA))
            need+=SocketIOHelper.NEED_CA;
        return need;
    }

    private ParamBuilder prepareHandShakeParam(String need){
        ParamBuilder result=new ParamBuilder();
        if(need.contains(SocketIOHelper.NEED_PUB_KEY)){
            result.putParam(SocketIOHelper.NEED_PUB_KEY,KeyUtils.getCurrentKeySet().getPub());
        }
        if(need.contains(SocketIOHelper.NEED_CA)){
            String caStr=CAUtils.readCAFile(parent);
            result.putParam(SocketIOHelper.NEED_CA,caStr);
        }

        return result;
    }

    private boolean checkHandShakeParam(Map<String,String> params,String myNeed){
        /**
         * 如果有问题就返回false，没问题就跳过
         */
        String dstPubKey=params.get(SocketIOHelper.NEED_PUB_KEY);
        if(myNeed.contains(SocketIOHelper.NEED_PUB_KEY)){
            if(TextUtils.isEmpty(dstPubKey)){
                log.error("对方公钥为空");
                return false;
            }

            helper.initRSACryptModule(dstPubKey,KeyUtils.getCurrentKeySet().getPri());
        }
        if(myNeed.contains(SocketIOHelper.NEED_CA)){
            String ca=params.get(SocketIOHelper.NEED_CA);
            if(TextUtils.isEmpty(ca)){
                log.error("证书为空");
                return false;
            }

            CAObject caObject=CAUtils.stringToCAObject(ca);
            if(!CAUtils.checkCA(caObject,currentIp,dstPubKey)){
                return false;
            }

        }

        return true;
    }

    private boolean doHandShake(){
        String feedback=Datagram.HANDSHAKE_FEEDBACK_SUCCESS;
        /**
         * 1.发送需求
         * 2.获取需求
         * 3.发送对方需要的
         * 4.接收自己需要的
         * 5.反馈握手信息（如 成功 证书错误 等）
         */
        String myNeed=getNeed();
        if(!helper.sendNeed(myNeed)){
            log.error("发送需求失败");
            return false;
        }

        String dstNeed;
        if((dstNeed=helper.readNeed())==null){
            log.error("读取对方需求失败");
            return false;
        }

        ParamBuilder handShakeParam=prepareHandShakeParam(dstNeed);
        if(!helper.sendHandShakeParam(handShakeParam)){
            log.error("发送握手参数失败");
            return false;
        }

        Map<String,String> params;
        if((params=helper.readHandShakeParam())==null){
            log.error("读取对方握手参数失败");
            return false;
        }

        if(!checkHandShakeParam(params,myNeed)){
            log.error("参数检查失败");

            Intent intent=new Intent(parent, SettingsActivity.class);
            intent.putExtra("toast","校验服务器证书失败，如需要连接则需关闭服务器证书校验");
            parent.startActivity(intent);
            stopConnection();

            feedback=Datagram.HANDSHAKE_FEEDBACK_CA_WRONG;
            helper.sendFeedback(feedback);
            return false;
        }

        helper.sendFeedback(feedback);

        return true;
    }

    private boolean readHandShakeFeedback(){
        Datagram datagram=helper.readHandShakeFeedback();
        String feedback=datagram.getParamsAsString().get("feedback");
        if(TextUtils.isEmpty(feedback))
            return false;

        if(feedback.equalsIgnoreCase(Datagram.HANDSHAKE_FEEDBACK_SUCCESS)){
            return true;
        }else if(feedback.equalsIgnoreCase(Datagram.HANDSHAKE_FEEDBACK_CA_WRONG)){

            Intent intent=new Intent(parent, SettingsActivity.class);
            intent.putExtra("toast","本地证书有误，请检查");
            parent.startActivity(intent);
            stopConnection();

            return false;
        }
        return false;
    }

    private void doProcess() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(currentIp, MessageLoopService.SERVER_PORT), 10000);
            helper = new SocketIOHelper(socket.getInputStream(), socket.getOutputStream());

            if(!doHandShake())
                return;
            //读取反馈
            if(!readHandShakeFeedback())
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