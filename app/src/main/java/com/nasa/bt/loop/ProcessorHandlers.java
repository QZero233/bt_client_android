package com.nasa.bt.loop;

import android.content.Context;

import com.alibaba.fastjson.JSON;
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
import com.nasa.bt.upgrade.UpgradeStatus;
import com.nasa.bt.upgrade.UpgradeUtils;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.NotificationUtils;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ProcessorHandlers {

    private static final Logger log= AppLogConfigurator.getLogger();

    private Context context;

    private MessageDao messageDao;
    private UserInfoDao userInfoDao;
    private SessionDao sessionDao;
    private NotificationUtils notificationUtils;

    private static final int ID_LENGTH=Datagram.ID_LENGTH;

    /**
     * 已经向服务器申请具体内容的id
     */
    private Map<String,Boolean> idSent =new HashMap<>();

    private DatagramListener defaultUserInfoListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
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

    private DatagramListener defaultMessageIndexListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            String index=datagram.getParamsAsString().get("index");
            for(int i=0;i<index.length()/ID_LENGTH;i++){
                String id=index.substring(i*ID_LENGTH,(i+1)*ID_LENGTH);
                if(!checkSent(id))
                    continue;

                Datagram getDatagram=new Datagram(Datagram.IDENTIFIER_MESSAGE_DETAIL,new ParamBuilder().putParam("msg_id",id).build());
                SendDatagramUtils.sendDatagram(getDatagram);
                addSent(id);
            }
        }
    };

    private DatagramListener defaultMessageListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
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
                    SendDatagramUtils.sendDatagram(datagramUser);
                }else
                    notificationUtils.sendMessageNotification();
            }

            Datagram deleteDatagram=new Datagram(Datagram.IDENTIFIER_DELETE_MESSAGE,new ParamBuilder().putParam("msg_id",messageEntityGot.getMsgId()).build());
            SendDatagramUtils.sendDatagram(deleteDatagram);
        }
    };

    private ActionReportListener defaultSendMessageReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
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

    private ActionReportListener defaultAuthReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            if(actionReport.getActionStatus().equals("0")){
                //验证失败
                log.info("身份验证失败");
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_NAME,"");
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_CODE_HASH,"");
                return;
            }else{
                log.info("身份验证成功，开始发送未处理数据包");
                log.info("获得的uid "+actionReport.getMore());
                LocalSettingsUtils.save(context,LocalSettingsUtils.FIELD_UID,actionReport.getMore());
                //处理未发出的数据包
                SendDatagramUtils.sendUnsent();
            }
        }
    };

    private DatagramListener defaultSessionInfoListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
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

            /**
             * 若本地存在ID相同的对象，就更新其params属性，如果不存在则添加
             */
            log.debug("添加Session "+sessionEntity);
            sessionDao.addOrUpdateSession(sessionEntity);

            String dstUid= sessionEntity.getIdOfOther(myUid);
            if(userInfoDao.getUserInfoById(dstUid)==null){
                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_USER_INFO,new ParamBuilder().putParam("uid",dstUid).build());
                SendDatagramUtils.sendDatagram(datagramGet);
            }
        }
    };

    private DatagramListener defaultSessionIndexListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            Map<String,String> params=datagram.getParamsAsString();
            String sessionsId=params.get("session_id");
            for(int i=0;i<sessionsId.length()/ID_LENGTH;i++){
                String subId=sessionsId.substring(i*ID_LENGTH,(i+1)*ID_LENGTH);
                if(!checkSent(subId))
                    continue;

                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_SESSION_DETAIL,new ParamBuilder().putParam("session_id",subId).build());
                SendDatagramUtils.sendDatagram(datagramGet);
                addSent(subId);
            }
        }
    };

    private ActionReportListener defaultMarkReadReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            String msgId=actionReport.getReplyId();
            messageDao.markReadById(msgId);
        }
    };

    private DatagramListener defaultUpdateIndexListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            Map<String,String> params=datagram.getParamsAsString();
            String indexes=params.get("update_id");
            for(int i=0;i<indexes.length()/ID_LENGTH;i++){
                String subId=indexes.substring(i*ID_LENGTH,(i+1)*ID_LENGTH);
                if(!checkSent(subId))
                    continue;

                Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_UPDATE_DETAIL,new ParamBuilder().putParam("update_id",subId).build());
                SendDatagramUtils.sendDatagram(datagramGet);
                addSent(subId);
            }
        }
    };

    private DatagramListener defaultUpdateDetailListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            Map<String,String> params=datagram.getParamsAsString();
            UpdateEntity updateEntity=JSON.parseObject(params.get("update"),UpdateEntity.class);

            if(updateEntity==null)
                return;

            removeSent(updateEntity.getUpdateId());

            log.debug("收到更新 "+updateEntity);
            if(UpdateProcessor.processUpdate(updateEntity,context)){
                Datagram datagramDelete=new Datagram(Datagram.IDENTIFIER_DELETE_UPDATE,new ParamBuilder().putParam("update_id",updateEntity.getUpdateId()).build());
                SendDatagramUtils.sendDatagram(datagramDelete);
            }
        }
    };

    private DatagramListener defaultUpgradeVerCodeListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            int verCode=Integer.parseInt(datagram.getParamsAsString().get("ver_code"));
            int currentCode= UpgradeUtils.getVersionCode(context);
            if(currentCode<verCode){
                Datagram datagramSent=new Datagram(Datagram.IDENTIFIER_UPGRADE_DETAIL,null);
                SendDatagramUtils.sendDatagram(datagramSent);
            }else{
                UpgradeUtils.deleteTempUpgradeStatusFil(context);
            }
        }
    };

    private DatagramListener defaultUpgradeDetailListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            String json=datagram.getParamsAsString().get("upgrade_status");
            UpgradeUtils.writeTempUpgradeStatusFile(context,json);
        }
    };

    public ProcessorHandlers(Context context) {
        this.context = context;

        messageDao=new MessageDao(context);
        userInfoDao=new UserInfoDao(context);
        sessionDao=new SessionDao(context);
        notificationUtils=new NotificationUtils(context);
    }

    public void addDefaultIntents(){
        MessageLoopUtils.registerListenerDefault("DEFAULT_USER_INFO",Datagram.IDENTIFIER_USER_INFO,defaultUserInfoListener);
        MessageLoopUtils.registerListenerDefault("DEFAULT_MESSAGE",Datagram.IDENTIFIER_MESSAGE_DETAIL,defaultMessageListener);
        MessageLoopUtils.registerListenerDefault("DEFAULT_MESSAGE_INDEX",Datagram.IDENTIFIER_MESSAGE_INDEX,defaultMessageIndexListener);
        MessageLoopUtils.registerListenerDefault("DEFAULT_SESSION_INDEX",Datagram.IDENTIFIER_SESSIONS_INDEX,defaultSessionIndexListener);
        MessageLoopUtils.registerListenerDefault("DEFAULT_SESSION_DETAIL",Datagram.IDENTIFIER_SESSION_DETAIL,defaultSessionInfoListener);
        MessageLoopUtils.registerListenerDefault("DEFAULT_UPDATE_INDEX",Datagram.IDENTIFIER_UPDATE_INDEX,defaultUpdateIndexListener);
        MessageLoopUtils.registerListenerDefault("DEFAULT_UPDATE_DETAIL",Datagram.IDENTIFIER_UPDATE_DETAIL,defaultUpdateDetailListener);
        MessageLoopUtils.registerListenerDefault("DEFAULT_UPGRADE_VER_CODE",Datagram.IDENTIFIER_UPGRADE_VER_CODE,defaultUpgradeVerCodeListener);
        MessageLoopUtils.registerListenerDefault("DEFAULT_UPGRADE_DETAIL",Datagram.IDENTIFIER_UPGRADE_DETAIL,defaultUpgradeDetailListener);

        MessageLoopUtils.registerActionReportListenerDefault("DEFAULT_MESSAGE_STATUS",Datagram.IDENTIFIER_SEND_MESSAGE,defaultSendMessageReportListener);
        MessageLoopUtils.registerActionReportListenerDefault("DEFAULT_AUTH_REPORT",Datagram.IDENTIFIER_SIGN_IN,defaultAuthReportListener);
        MessageLoopUtils.registerActionReportListenerDefault("DEFAULT_MARK_READ_REPORT",Datagram.IDENTIFIER_MARK_READ,defaultMarkReadReportListener);
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
