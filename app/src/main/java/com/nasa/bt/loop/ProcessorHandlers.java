package com.nasa.bt.loop;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.nasa.bt.AuthInfoActivity;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.crypt.CryptModule;
import com.nasa.bt.crypt.CryptModuleFactory;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.socket.SocketIOHelper;
import com.nasa.bt.utils.CommonDbHelper;
import com.nasa.bt.utils.LocalDbUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

import java.util.HashMap;
import java.util.Map;

public class ProcessorHandlers {

    private Context context;

    private CommonDbHelper msgHelper,userHelper;

    private Handler defaultUserInfoProcessor=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Datagram datagram= (Datagram) msg.obj;

            Map<String,String> params=datagram.getParamsAsString();

            if(params.get("exist").equals("0"))
                return;

            String uid=params.get("uid");
            String name=params.get("name");
            UserInfo info=new UserInfo(name,uid);

            if(name.equals(LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_NAME)))
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_UID,uid);

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

            long time= SocketIOHelper.byteArrayToLong(datagram.getParams().get("time"));

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


    private Handler defaultSendMessageReportHandler =new Handler(){
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

    private Handler defaultAuthReportHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            if(!params.get("action_identifier").equalsIgnoreCase(Datagram.IDENTIFIER_SIGN_IN))
                return;

            if(params.get("action_status").equals("0")){
                //验证失败
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_NAME,"");
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_CODE_HASH,"");
                Toast.makeText(context,"身份验证失败，请重新输入信息",Toast.LENGTH_SHORT).show();
                context.startActivity(new Intent(context, AuthInfoActivity.class));
                return;
            }

            Toast.makeText(context,"身份验证成功",Toast.LENGTH_SHORT).show();
            pullUserInfo();
        }
    };



    private MessageIntent userInfoIntent=new MessageIntent("DEFAULT_USER_INFO",Datagram.IDENTIFIER_RETURN_USER_INFO,defaultUserInfoProcessor,0,0);
    private MessageIntent userIndexIntent=new MessageIntent("DEFAULT_USER_INDEX",Datagram.IDENTIFIER_RETURN_USERS_INDEX,defaultUserIndexProcessor,0,0);
    private MessageIntent messageIntent=new MessageIntent("DEFAULT_MESSAGE",Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,defaultMessageProcessor,0,0);
    private MessageIntent messageIndexIntent=new MessageIntent("DEFAULT_MESSAGE_INDEX",Datagram.IDENTIFIER_RETURN_MESSAGE_INDEX,defaultMessageIndexProcessor,0,0);
    private MessageIntent messageStatusIntent=new MessageIntent("DEFAULT_MESSAGE_STATUS",Datagram.IDENTIFIER_REPORT, defaultSendMessageReportHandler,0,0);
    private MessageIntent authReport=new MessageIntent("DEFAULT_AUTH_REPORT",Datagram.IDENTIFIER_REPORT, defaultAuthReportHandler,0,0);

    public ProcessorHandlers(Context context) {
        this.context = context;

        msgHelper= LocalDbUtils.getMsgHelper(context);
        userHelper=LocalDbUtils.getUserInfoHelper(context);
    }

    public void addDefaultIntents(){
        MessageLoop.addIntent(userInfoIntent);
        MessageLoop.addIntent(userIndexIntent);
        MessageLoop.addIntent(messageIntent);
        MessageLoop.addIntent(messageIndexIntent);
        MessageLoop.addIntent(messageStatusIntent);
        MessageLoop.addIntent(authReport);
    }

    private void pullUserInfo(){
        Datagram datagramPullUser=new Datagram(Datagram.IDENTIFIER_GET_USERS_INDEX,null);
        LoopResource.sendDatagram(datagramPullUser);
    }


}
