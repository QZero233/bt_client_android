package com.nasa.bt.loop;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.AuthInfoActivity;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.utils.LocalSettingsUtils;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ProcessorHandlers {

    private static final Logger log= AppLogConfigurator.getLogger();

    private Context context;

    private MessageDao messageDao;
    private UserInfoDao userInfoDao;
    private SessionDao sessionDao;

    /**
     * 已经向服务器申请具体内容的id
     */
    private Map<String,Boolean> idSent =new HashMap<>();

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
            UserInfoEntity info=new UserInfoEntity(uid,name);

            if(name.equals(LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_NAME)))
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_UID,uid);

            userInfoDao.addUser(info);
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
                if(!checkSent(id))
                    continue;

                Map<String,byte[]> getParams=new HashMap<>();
                getParams.put("msg_id",id.getBytes());
                Datagram getDatagram=new Datagram(Datagram.IDENTIFIER_GET_MESSAGE_DETAIL,getParams);
                LoopResource.sendDatagram(getDatagram);
                addSent(id);
            }

        }
    };

    private Handler defaultMessageProcessor=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Datagram datagram= (Datagram) msg.obj;

            Map<String,String> params=datagram.getParamsAsString();
            MessageEntity messageEntityGot = JSON.parseObject(params.get("msg"), MessageEntity.class);
            messageEntityGot.setStatus(MessageEntity.STATUS_UNREAD);

            log.debug("收到消息 "+ messageEntityGot);
            removeSent(messageEntityGot.getMsgId());

            if(messageEntityGot.getSrcUid().equals("system")){
                messageDao.markReadById(messageEntityGot.getContent());
            }else{
                messageDao.addMessage(messageEntityGot);
                sessionDao.changeLastStatus(messageEntityGot.getSessionId(),messageEntityGot.getContent(),messageEntityGot.getTime());

                if(sessionDao.getSessionById(messageEntityGot.getSessionId())==null){
                    Map<String,String> paramsUser=new HashMap<>();
                    paramsUser.put("session_id", messageEntityGot.getSessionId());
                    Datagram datagramUser=new Datagram(Datagram.IDENTIFIER_GET_SESSION_DETAIL,paramsUser,"");
                    LoopResource.sendDatagram(datagramUser);
                }
            }

            Map<String,byte[]> deleteParams=new HashMap<>();
            deleteParams.put("msg_id", messageEntityGot.getMsgId().getBytes());
            Datagram deleteDatagram=new Datagram(Datagram.IDENTIFIER_DELETE_MESSAGE,deleteParams);
            LoopResource.sendDatagram(deleteDatagram);
           // NotificationUtils.sendNotification(context);
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
                status= MessageEntity.STATUS_FAILED;
            else
                status= MessageEntity.STATUS_UNREAD;

            String id=actionReport.getReplyId();
            messageDao.changeMessageStatusById(id,status);

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


    private Handler defaultSessionInfoHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            String sessionStr=params.get("sessionEntity");
            SessionEntity sessionEntity =JSON.parseObject(sessionStr, SessionEntity.class);
            if(sessionEntity ==null)
                return;

            removeSent(sessionEntity.getSessionId());

            String myUid=LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_UID);
            if(!sessionEntity.checkInSession(myUid))
                return;

            sessionDao.addSession(sessionEntity);
            /**
             * FIXME 如果本地数据库中存在ID相同的对象，就不会进行任何更改操作（包括更新会话信息），但是如果覆盖本地就会导致一些本地数据丢失（比如最近信息）
             */
            /*
            SessionEntity sessionEntityLocal = (SessionEntity) sessionHelper.querySingle("SELECT * FROM sessionEntity WHERE sessionId='"+ sessionEntity.getSessionId()+"'");
            if(sessionEntityLocal ==null)
                sessionHelper.insert(sessionEntity);
            else{
                sessionEntity.setLastTime(sessionEntityLocal.getLastTime());
                sessionHelper.update(sessionEntity, sessionEntity);
            }
            */



            String dstUid= sessionEntity.getIdOfOther(myUid);
            if(userInfoDao.getUserInfoById(dstUid)==null){
                Map<String,String> paramsGet=new HashMap<>();
                paramsGet.put("uid",dstUid);
                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_GET_USER_INFO,paramsGet,null);
                LoopResource.sendDatagram(datagramGet);
            }

        }
    };

    private Handler defaultSessionIndexHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            String sessionsId=params.get("session_id");
            for(int i=0;i<sessionsId.length()/36;i++){
                String subId=sessionsId.substring(i*36,(i+1)*36);
                if(!checkSent(subId))
                    continue;

                Map<String,String> paramsGet=new HashMap<>();
                paramsGet.put("session_id",subId);
                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_GET_SESSION_DETAIL,paramsGet,null);
                LoopResource.sendDatagram(datagramGet);
                addSent(subId);
            }

        }
    };

    private Handler defaultMarkReadReportHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            ActionReport report=JSON.parseObject(params.get("action_report"),ActionReport.class);
            if(report==null || !report.getActionIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_MARK_READ))
                return;

            String msgId=report.getReplyId();
            messageDao.markReadById(msgId);
        }
    };

    private MessageIntent userInfoIntent=new MessageIntent("DEFAULT_USER_INFO",Datagram.IDENTIFIER_RETURN_USER_INFO,defaultUserInfoProcessor,0,0);
    private MessageIntent messageIntent=new MessageIntent("DEFAULT_MESSAGE",Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,defaultMessageProcessor,0,0);
    private MessageIntent messageIndexIntent=new MessageIntent("DEFAULT_MESSAGE_INDEX",Datagram.IDENTIFIER_RETURN_MESSAGE_INDEX,defaultMessageIndexProcessor,0,0);
    private MessageIntent messageStatusIntent=new MessageIntent("DEFAULT_MESSAGE_STATUS",Datagram.IDENTIFIER_REPORT, defaultSendMessageReportHandler,0,0);
    private MessageIntent authReportIntent=new MessageIntent("DEFAULT_AUTH_REPORT",Datagram.IDENTIFIER_REPORT, defaultAuthReportHandler,0,0);
    private MessageIntent markReadReportIntent=new MessageIntent("DEFAULT_MARK_READ_REPORT",Datagram.IDENTIFIER_REPORT, defaultMarkReadReportHandler,0,0);
    private MessageIntent sessionIndexIntent=new MessageIntent("DEFAULT_SESSION_INDEX",Datagram.IDENTIFIER_RETURN_SESSIONS_INDEX, defaultSessionIndexHandler,0,0);
    private MessageIntent sessionDetailIntent=new MessageIntent("DEFAULT_SESSION_DETAIL",Datagram.IDENTIFIER_RETURN_SESSION_DETAIL, defaultSessionInfoHandler,0,0);

    public ProcessorHandlers(Context context) {
        this.context = context;

        messageDao=new MessageDao(context);
        userInfoDao=new UserInfoDao(context);
        sessionDao=new SessionDao(context);
    }

    public void addDefaultIntents(){
        MessageLoop.addIntent(userInfoIntent);
        MessageLoop.addIntent(messageIntent);
        MessageLoop.addIntent(messageIndexIntent);
        MessageLoop.addIntent(messageStatusIntent);
        MessageLoop.addIntent(authReportIntent);
        MessageLoop.addIntent(markReadReportIntent);
        MessageLoop.addIntent(sessionDetailIntent);
        MessageLoop.addIntent(sessionIndexIntent);
    }

    private boolean checkSent(String msgId){
        synchronized (idSent){
            if(idSent.get(msgId)!=null)
                return false;
            return true;
        }
    }

    private void addSent(String msgId){
        synchronized (idSent){
            idSent.put(msgId,new Boolean(true));
        }
    }

    private void removeSent(String msgId){
        synchronized (idSent){
            idSent.remove(msgId);
        }
    }
}
