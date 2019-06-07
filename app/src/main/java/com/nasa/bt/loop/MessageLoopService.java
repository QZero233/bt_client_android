package com.nasa.bt.loop;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.LoginInfo;
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.socket.SocketIOHelper;
import com.nasa.bt.utils.CommonDbHelper;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.UUIDUtils;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MessageLoopService extends Service implements Runnable {

    private static final String SERVER_IP="10.0.2.2";
    private static final int SERVER_PORT=8848;
    private Socket socket;
    private SocketIOHelper helper;

    public static MessageLoopService instance=null;

    public boolean needReConnect=false;
    private boolean running=true;

    private CommonDbHelper msgHelper,userHelper;

    private Handler defaultUserInfoProcessor=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Datagram datagram= (Datagram) msg.obj;

            Map<String,String> params=datagram.getParamsAsString();
            String uid=params.get("uid");
            String name=params.get("name");
            String key=params.get("key");
            UserInfo info=new UserInfo(name,uid,key);

            if(name.equals(LocalSettingsUtils.read(MessageLoopService.this,LocalSettingsUtils.FIELD_NAME)))
                LocalSettingsUtils.save(MessageLoopService.this,LocalSettingsUtils.FIELD_UID,uid);

            userHelper.execSql("DELETE FROM userinfo WHERE id='"+uid+"'");
            userHelper.insert(info);
        }
    };

    private Handler defaultUserIndexProcessor=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;

            String index=datagram.getParamsAsString().get("index");
            for(int i=0;i<index.length()/36;i++){
                String id=index.substring(i*36,(i+1)*36);

                Map<String,byte[]> getParams=new HashMap<>();
                getParams.put("uid",id.getBytes());
                Datagram getDatagram=new Datagram(Datagram.IDENTIFIER_GET_USER_INFO,getParams);
                LoopResource.sendDatagram(getDatagram);
            }

        }
    };

    private Handler defaultMessageIndexProcessor=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;

            String index=datagram.getParamsAsString().get("index");
            for(int i=0;i<index.length()/36;i++){
                String id=index.substring(i*36,(i+1)*36);

                Map<String,byte[]> getParams=new HashMap<>();
                getParams.put("msg_id",id.getBytes());
                Datagram getDatagram=new Datagram(Datagram.IDENTIFIER_GET_MESSAGE_DETAIL,getParams);
                LoopResource.sendDatagram(getDatagram);
            }

        }
    };

    private Handler defaultMessageProcessor=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Datagram datagram= (Datagram) msg.obj;

            Map<String,String> params=datagram.getParamsAsString();
            String msgId=params.get("msg_id");
            String srcId=params.get("src_uid");
            String msgContent=params.get("msg_content");
            long time=SocketIOHelper.byteArrayToLong(datagram.getParams().get("time"));

            Msg msgGot=new Msg(msgId,srcId,"",msgContent,time,Msg.STATUS_UNREAD);
            if(srcId.equals("system")){
                msgHelper.execSql("UPDATE msg SET status="+Msg.STATUS_READ+" WHERE msgId='"+msgGot.getContent()+"'");
            }else{
                msgHelper.insert(msgGot);
            }

            Map<String,byte[]> deleteParams=new HashMap<>();
            deleteParams.put("msg_id",msgId.getBytes());
            Datagram deleteDatagram=new Datagram(Datagram.IDENTIFIER_DELETE_MESSAGE,deleteParams);
            LoopResource.sendDatagram(deleteDatagram);
        }
    };

    private Handler sendMessageReportHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            if(!params.get("action_identifier").equalsIgnoreCase(Datagram.IDENTIFIER_SEND_MESSAGE))
                return;

            int status;
            if(params.get("action_status").equals("0"))
                status=Msg.STATUS_FAILED;
            else
                status=Msg.STATUS_UNREAD;

            String id=params.get("reply_id");
            msgHelper.execSql("UPDATE msg SET status="+status+" WHERE msgId='"+id+"'");
        }
    };

    private Handler reconnectHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            needReConnect=true;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running=false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance=this;
        msgHelper=new CommonDbHelper(this,Msg.class,"");
        userHelper=new CommonDbHelper(this, UserInfo.class,"");
        new Thread(this).start();
        new Thread(){
            @Override
            public void run() {
                super.run();

                while(true){
                    try {
                        Thread.sleep(5000);
                        if(needReConnect){
                            Log.e("NASA","开始重连");
                            reConnect();
                            needReConnect=false;
                        }
                    }catch (Exception e){

                    }
                }

            }
        }.start();

        MessageIntent userInfoIntent=new MessageIntent("DEFAULT_USER_INFO",Datagram.IDENTIFIER_RETURN_USER_INFO,defaultUserInfoProcessor,0,0);
        MessageIntent userIndexIntent=new MessageIntent("DEFAULT_USER_INDEX",Datagram.IDENTIFIER_RETURN_USERS_INDEX,defaultUserIndexProcessor,0,0);
        MessageIntent messageIntent=new MessageIntent("DEFAULT_MESSAGE",Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,defaultMessageProcessor,0,0);
        MessageIntent messageIndexIntent=new MessageIntent("DEFAULT_MESSAGE_INDEX",Datagram.IDENTIFIER_RETURN_MESSAGE_INDEX,defaultMessageIndexProcessor,0,0);
        MessageIntent messageStatusIntent=new MessageIntent("DEFAULT_MESSAGE_STATUS",Datagram.IDENTIFIER_REPORT,sendMessageReportHandler,0,0);

        MessageIntent reconnectIntent=new MessageIntent("SERVICE_RECONNECT",LoopResource.INBOX_IDENTIFIER_RECONNECT,reconnectHandler,0,0);

        MessageLoop.addIntent(userInfoIntent);
        MessageLoop.addIntent(userIndexIntent);
        MessageLoop.addIntent(messageIntent);
        MessageLoop.addIntent(messageIndexIntent);
        MessageLoop.addIntent(reconnectIntent);
        MessageLoop.addIntent(messageStatusIntent);
    }

    public void reConnect(){
        try {
            socket.close();
        }catch (Exception e){
        }

        new Thread(this).start();
    }

    public boolean sendDatagram(Datagram datagram){
        if(socket.isClosed() || !socket.isConnected())
            return false;

        try{
            return helper.writeOs(datagram);
        }catch (Exception e){
            return false;
        }
    }

    private boolean doAuth(){
        LoginInfo info=LoopResource.loginInfo;
        if(info==null){
            info=new LoginInfo();

            String sid= LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_SID);
            String name= LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_NAME);
            String code= LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_CODE_HASH);

            if(!TextUtils.isEmpty(sid)){
                info.sid=sid;
            }else if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(code)){
                info.name=name;
                info.codeHash=code;
            }else{
                return false;
            }

        }

        Map<String,byte[]> loginParam=new HashMap<>();
        if(info.sid!=null){
            //SID登录
            loginParam.put("use_sid","yes".getBytes());
            loginParam.put("sid",info.sid.getBytes());
        }else{
            //用户名密码登录
            loginParam.put("use_sid","no".getBytes());
            loginParam.put("username",info.name.getBytes());
            loginParam.put("code_hash",info.codeHash.getBytes());
        }

        Datagram loginDatagram=new Datagram(Datagram.IDENTIFIER_SIGN_IN,loginParam);
        return helper.writeOs(loginDatagram);
    }

    @Override
    public void run() {
        try {
            socket=new Socket(SERVER_IP,SERVER_PORT);
            helper=new SocketIOHelper(socket.getInputStream(),socket.getOutputStream());
            Log.e("NASA","连接完成，开始进行身份验证");
            if(!doAuth()){
                Log.e("NASA","身份验证失败，继续准备重连");
                needReConnect=true;
                return;
            }

            //处理未发出的数据包
            LoopResource.sendUnsent();

            while(true){
                //开始循环监听
                Datagram datagram=helper.readIs();
                if(datagram==null)
                    throw new Exception();

                MessageLoop.processDatagram(datagram);
            }

        }catch (Exception e){
            Log.e("NASA","正在尝试断线重连 "+e.getMessage());
            needReConnect=true;
        }

    }
}
