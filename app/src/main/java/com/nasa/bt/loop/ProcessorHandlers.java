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
import com.nasa.bt.cls.SecretChat;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.socket.SocketIOHelper;
import com.nasa.bt.utils.CommonDbHelper;
import com.nasa.bt.utils.LocalDbUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ProcessorHandlers {

    private static final Logger log= AppLogConfigurator.getLogger();

    private Context context;

    private CommonDbHelper msgHelper,userHelper,secretChatHelper;

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

            log.debug("收到消息 "+msgGot);

            if(msgGot.getSrcUid().equals("system")){
                msgHelper.execSql("UPDATE msg SET status="+Msg.STATUS_READ+" WHERE msgId='"+msgGot.getContent()+"'");
            }else if(msgGot.getSrcUid().equals("secretChat")){
                log.info("收到私密聊天创建 "+msgGot.getContent());
                Map<String,String> getParam=new HashMap<>();
                getParam.put("session_id",msgGot.getContent());
                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_GET_SECRET_CHAT,getParam,null);
                LoopResource.sendDatagram(datagramGet);
            }else if(msgGot.getSrcUid().equals("secretChatDelete")){
                secretChatHelper.execSql("UPDATE secretchat SET status="+SecretChat.STATUS_CLOSED+" WHERE sessionId='"+msgGot.getContent()+"'");
            } else{
                msgHelper.execSql("DELETE FROM msg WHERE msgId='"+msgGot.getMsgId()+"'");
                msgHelper.insert(msgGot);

                if(msgGot.getMsgType().equals(Msg.MSG_TYPE_NORMAL)){
                    if(userHelper.querySingle("SELECT * FROM userinfo WHERE id='"+msgGot.getSrcUid()+"'")==null){
                        Map<String,String> paramsUser=new HashMap<>();
                        paramsUser.put("uid",msgGot.getSrcUid());
                        Datagram datagramUser=new Datagram(Datagram.IDENTIFIER_GET_USER_INFO,paramsUser,"");
                        LoopResource.sendDatagram(datagramUser);
                    }
                }else if(msgGot.getMsgType().equals(Msg.MSG_TYPE_SECRET_1)){
                    if(secretChatHelper.querySingle("SELECT * FROM secretchat WHERE sessionId='"+msgGot.getSrcUid()+"'")==null){
                        log.debug("收到未知私密聊天，请求服务器......"+msgGot.getSrcUid());
                        Map<String,String> paramsUser=new HashMap<>();
                        paramsUser.put("session_id",msgGot.getSrcUid());
                        Datagram datagramUser=new Datagram(Datagram.IDENTIFIER_GET_SECRET_CHAT,paramsUser,"");
                        LoopResource.sendDatagram(datagramUser);
                    }
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

            log.debug("消息 "+id+" 状态反馈 "+status);
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
                log.info("身份验证失败");
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_NAME,"");
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_CODE_HASH,"");
                Toast.makeText(context,"身份验证失败，请重新输入信息",Toast.LENGTH_SHORT).show();
                context.startActivity(new Intent(context, AuthInfoActivity.class));
                return;
            }else{
                log.info("身份验证成功，开始发送未处理数据包");
                log.info("获得的uid "+actionReport.getMore());
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_UID,actionReport.getMore());
                //处理未发出的数据包
                LoopResource.sendUnsent();
            }
        }
    };

    private Handler defaultSecretChatIndexHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();

            String indexes=params.get("session_index");
            for(int i=0;i<indexes.length()/36;i++){
                String index=indexes.substring(i*36,(i+1)*36);

                Map<String,String> getParam=new HashMap<>();
                getParam.put("session_id",index);
                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_GET_SECRET_CHAT,getParam,null);
                LoopResource.sendDatagram(datagramGet);
            }
        }
    };

    private Handler defaultSecretChatHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            SecretChat secretChat=JSON.parseObject(params.get("secret_chat"),SecretChat.class);
            secretChatHelper.insert(secretChat);
        }
    };

    private MessageIntent userInfoIntent=new MessageIntent("DEFAULT_USER_INFO",Datagram.IDENTIFIER_RETURN_USER_INFO,defaultUserInfoProcessor,0,0);
    private MessageIntent userIndexIntent=new MessageIntent("DEFAULT_USER_INDEX",Datagram.IDENTIFIER_RETURN_USERS_INDEX,defaultUserIndexProcessor,0,0);
    private MessageIntent messageIntent=new MessageIntent("DEFAULT_MESSAGE",Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,defaultMessageProcessor,0,0);
    private MessageIntent messageIndexIntent=new MessageIntent("DEFAULT_MESSAGE_INDEX",Datagram.IDENTIFIER_RETURN_MESSAGE_INDEX,defaultMessageIndexProcessor,0,0);
    private MessageIntent messageStatusIntent=new MessageIntent("DEFAULT_MESSAGE_STATUS",Datagram.IDENTIFIER_REPORT, defaultSendMessageReportHandler,0,0);
    private MessageIntent authReport=new MessageIntent("DEFAULT_AUTH_REPORT",Datagram.IDENTIFIER_REPORT, defaultAuthReportHandler,0,0);
    private MessageIntent secretChatIndex=new MessageIntent("DEFAULT_SECRET_CHAT_INDEX",Datagram.IDENTIFIER_RETURN_SECRET_CHAT_INDEX, defaultSecretChatIndexHandler,0,0);
    private MessageIntent secretChat=new MessageIntent("DEFAULT_SECRET_CHAT",Datagram.IDENTIFIER_RETURN_SECRET_CHAT, defaultSecretChatHandler,0,0);

    public ProcessorHandlers(Context context) {
        this.context = context;

        msgHelper= LocalDbUtils.getMsgHelper(context);
        userHelper=LocalDbUtils.getUserInfoHelper(context);
        secretChatHelper=LocalDbUtils.getSecretChatHelper(context);
    }

    public void addDefaultIntents(){
        MessageLoop.addIntent(userInfoIntent);
        MessageLoop.addIntent(userIndexIntent);
        MessageLoop.addIntent(messageIntent);
        MessageLoop.addIntent(messageIndexIntent);
        MessageLoop.addIntent(messageStatusIntent);
        MessageLoop.addIntent(authReport);
        MessageLoop.addIntent(secretChatIndex);
        MessageLoop.addIntent(secretChat);
    }
}
