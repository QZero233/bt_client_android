package com.nasa.bt.loop;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.BugTelegramApplication;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UpdateEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.update.UpdateProcessor;
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

    private BugTelegramApplication application;

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

                Datagram getDatagram=new Datagram(Datagram.IDENTIFIER_MESSAGE_DETAIL,new ParamBuilder().putParam("msg_id",id).build());
                MessageLoopResource.sendDatagram(getDatagram);
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
                    Datagram datagramUser=new Datagram(Datagram.IDENTIFIER_SESSION_DETAIL,new ParamBuilder().putParam("session_id",messageEntityGot.getSessionId()).build());
                    MessageLoopResource.sendDatagram(datagramUser);
                }
            }

            Datagram deleteDatagram=new Datagram(Datagram.IDENTIFIER_DELETE_MESSAGE,new ParamBuilder().putParam("msg_id",messageEntityGot.getMsgId()).build());
            MessageLoopResource.sendDatagram(deleteDatagram);
            //TODO 通知
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
                return;
            }else{
                application.setConnectionStatus(MessageLoopService.STATUS_CONNECTED);
                log.info("身份验证成功，开始发送未处理数据包");
                log.info("获得的uid "+actionReport.getMore());
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_UID,actionReport.getMore());
                //处理未发出的数据包
                MessageLoopResource.sendUnsent();
            }
        }
    };


    private Handler defaultSessionInfoHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            String sessionStr=params.get("session");
            SessionEntity sessionEntity =JSON.parseObject(sessionStr, SessionEntity.class);
            if(sessionEntity ==null)
                return;

            removeSent(sessionEntity.getSessionId());


            String myUid=LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_UID);
            log.debug("收到Session "+sessionEntity);
            log.debug("uid "+myUid);
            if(!sessionEntity.checkInSession(myUid))
                return;

            log.debug("添加Session "+sessionEntity);
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
                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_USER_INFO,new ParamBuilder().putParam("uid",dstUid).build());
                MessageLoopResource.sendDatagram(datagramGet);
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

                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_SESSION_DETAIL,new ParamBuilder().putParam("session_id",subId).build());
                MessageLoopResource.sendDatagram(datagramGet);
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

    private Handler defaultUpdateIndexHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            String indexes=params.get("update_id");
            for(int i=0;i<indexes.length()/36;i++){
                String subId=indexes.substring(i*36,(i+1)*36);
                if(!checkSent(subId))
                    continue;

                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_UPDATE_DETAIL,new ParamBuilder().putParam("update_id",subId).build());
                MessageLoopResource.sendDatagram(datagramGet);
                addSent(subId);
            }
        }
    };

    private Handler defaultUpdateDetailHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            UpdateEntity updateEntity=JSON.parseObject(params.get("update"),UpdateEntity.class);

            if(updateEntity==null)
                return;

            removeSent(updateEntity.getUpdateId());

            log.debug("收到更新 "+updateEntity);
            if(UpdateProcessor.processUpdate(updateEntity,context)){
                Datagram datagramDelete=new Datagram(Datagram.IDENTIFIER_DELETE_UPDATE,new ParamBuilder().putParam("update_id",updateEntity.getUpdateId()).build());
                MessageLoopResource.sendDatagram(datagramDelete);
            }
        }
    };

    private MessageIntent userInfoIntent=new MessageIntent("DEFAULT_USER_INFO",Datagram.IDENTIFIER_USER_INFO,defaultUserInfoProcessor,0,0);
    private MessageIntent messageIntent=new MessageIntent("DEFAULT_MESSAGE",Datagram.IDENTIFIER_MESSAGE_DETAIL,defaultMessageProcessor,0,0);
    private MessageIntent messageIndexIntent=new MessageIntent("DEFAULT_MESSAGE_INDEX",Datagram.IDENTIFIER_MESSAGE_INDEX,defaultMessageIndexProcessor,0,0);
    private MessageIntent messageStatusIntent=new MessageIntent("DEFAULT_MESSAGE_STATUS",Datagram.IDENTIFIER_REPORT, defaultSendMessageReportHandler,0,0);
    private MessageIntent authReportIntent=new MessageIntent("DEFAULT_AUTH_REPORT",Datagram.IDENTIFIER_REPORT, defaultAuthReportHandler,0,0);
    private MessageIntent markReadReportIntent=new MessageIntent("DEFAULT_MARK_READ_REPORT",Datagram.IDENTIFIER_REPORT, defaultMarkReadReportHandler,0,0);
    private MessageIntent sessionIndexIntent=new MessageIntent("DEFAULT_SESSION_INDEX",Datagram.IDENTIFIER_SESSIONS_INDEX, defaultSessionIndexHandler,0,0);
    private MessageIntent sessionDetailIntent=new MessageIntent("DEFAULT_SESSION_DETAIL",Datagram.IDENTIFIER_SESSION_DETAIL, defaultSessionInfoHandler,0,0);
    private MessageIntent updateIndexIntent=new MessageIntent("DEFAULT_UPDATE_INDEX",Datagram.IDENTIFIER_UPDATE_INDEX,defaultUpdateIndexHandler,0,0);
    private MessageIntent updateDetailIntent=new MessageIntent("DEFAULT_UPDATE_DETAIL",Datagram.IDENTIFIER_UPDATE_DETAIL,defaultUpdateDetailHandler,0,0);

    public ProcessorHandlers(Context context,BugTelegramApplication application) {
        this.context = context;
        this.application=application;

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
        MessageLoop.addIntent(updateIndexIntent);
        MessageLoop.addIntent(updateDetailIntent);
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
