package com.nasa.bt.loop;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.AuthInfoActivity;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.UserInfo;
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
            Msg msgGot= JSON.parseObject(params.get("msg"),Msg.class);
            msgGot.setStatus(Msg.STATUS_UNREAD);

            if(msgGot.getSrcUid().equals("system")){
                msgHelper.execSql("UPDATE msg SET status="+Msg.STATUS_READ+" WHERE msgId='"+msgGot.getContent()+"'");
            }else{
                msgHelper.insert(msgGot);

                if(userHelper.querySingle("SELECT * FROM userinfo WHERE id='"+msgGot.getSrcUid()+"'")==null){
                    Log.e("NASA","未知用户 "+msgGot.getSrcUid());
                    Map<String,String> paramsUser=new HashMap<>();
                    paramsUser.put("uid",msgGot.getSrcUid());
                    Datagram datagramUser=new Datagram(Datagram.IDENTIFIER_GET_USER_INFO,paramsUser,"");
                    LoopResource.sendDatagram(datagramUser);
                }

            }

            Map<String,byte[]> deleteParams=new HashMap<>();
            deleteParams.put("msg_id",msgGot.getMsgId().getBytes());
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
            ActionReport actionReport=JSON.parseObject(params.get("action_report"),ActionReport.class);

            if(!actionReport.getActionIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_SEND_MESSAGE))
                return;

            int status;
            if(actionReport.getActionStatus().equals("0"))
                status=Msg.STATUS_FAILED;
            else
                status=Msg.STATUS_UNREAD;

            String id=actionReport.getReplyId();
            msgHelper.execSql("UPDATE msg SET status="+status+" WHERE msgId='"+id+"'");
            Log.e("nasa","消息 "+id+" 状态反馈 "+status);
        }
    };

    private Handler defaultAuthReportHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            ActionReport actionReport=JSON.parseObject(params.get("action_report"),ActionReport.class);

            if(!actionReport.getActionIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_SIGN_IN))
                return;

            if(actionReport.getActionStatus().equals("0")){
                //验证失败
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_NAME,"");
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_CODE_HASH,"");
                Toast.makeText(context,"身份验证失败，请重新输入信息",Toast.LENGTH_SHORT).show();
                context.startActivity(new Intent(context, AuthInfoActivity.class));
                return;
            }
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
}
