package com.nasa.bt.model;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.contract.SessionListContract;
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.SessionEntityForShow;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.loop.ActionReportListener;
import com.nasa.bt.loop.DatagramListener;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.upgrade.UpgradeStatus;
import com.nasa.bt.utils.LocalSettingsUtils;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class SessionListModel implements SessionListContract.Model {

    private Context context;
    private SessionDao sessionDao;
    private MessageDao messageDao;
    private UserInfoDao userInfoDao;
    private static final Logger log = AppLogConfigurator.getLogger();

    public SessionListModel(Context context) {
        this.context = context;
        sessionDao=new SessionDao(context);
        messageDao=new MessageDao(context);
        userInfoDao=new UserInfoDao(context);
    }


    @Override
    public void doSync(final SessionListContract.Callback callback) {
        List<SessionEntity> sessionEntities=sessionDao.getAllSession();
        String ids="";
        for(SessionEntity sessionEntity:sessionEntities){
            ids+=sessionEntity.getSessionId();
        }

        long lastSyncTime=LocalSettingsUtils.readLong(context, LocalSettingsUtils.FIELD_LAST_SYNC_TIME);

        Datagram datagram=new Datagram(Datagram.IDENTIFIER_SYNC,new ParamBuilder().putParam("session_id",ids).
                putParam("last_sync_time", lastSyncTime+"").build());
        SendDatagramUtils.sendDatagram(datagram);
        MessageLoopUtils.registerSpecifiedTimesActionReportListener("SESSION_LIST_SYNC_REPORT", Datagram.IDENTIFIER_SYNC, 1,
                new ActionReportListener() {
                    @Override
                    public void onActionReportReach(ActionReport actionReport) {
                        if(actionReport.getActionStatusInBoolean())
                            callback.onSuccess();
                        else
                            callback.onFailure(0);
                    }
                });
    }

    @Override
    public void doRefresh(final SessionListContract.Callback callback) {
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_REFRESH,null);
        SendDatagramUtils.sendDatagram(datagram);
        MessageLoopUtils.registerSpecifiedTimesActionReportListener("SESSION_LIST_REFRESH_REPORT", Datagram.IDENTIFIER_REFRESH, 1,
                new ActionReportListener() {
                    @Override
                    public void onActionReportReach(ActionReport actionReport) {
                        if(actionReport.getActionStatusInBoolean())
                            callback.onSuccess();
                        else
                            callback.onFailure(0);
                    }
                });
    }

    @Override
    public List<SessionEntityForShow> getAllSessions() {
        List<SessionEntity> sessionEntityList=sessionDao.getAllSession();
        List<SessionEntityForShow> result=new ArrayList<>();

        String uid=LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_UID);
        for(SessionEntity sessionEntity:sessionEntityList){
            String dstUid=sessionEntity.getIdOfOther(uid);
            UserInfoEntity userInfoEntity=userInfoDao.getUserInfoById(dstUid);

            int unreadCount=0;
            MessageEntity latestUnreadMessage=null;
            List<MessageEntity> unreadMessages =messageDao.getUnreadMessageBySessionId(sessionEntity.getSessionId());
            if(unreadMessages!=null && !unreadMessages.isEmpty()){
                unreadCount=unreadMessages.size();
                latestUnreadMessage=unreadMessages.get(0);
            }

            SessionEntityForShow sessionEntityForShow=new SessionEntityForShow(sessionEntity.getSessionId(),sessionEntity,userInfoEntity,unreadCount,latestUnreadMessage);
            result.add(sessionEntityForShow);
        }

        log.debug("sessions::"+result);
        return result;
    }

    @Override
    public String getDrawerInfo() {
        String uid=LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_UID);
        if(TextUtils.isEmpty(uid))
            return null;

        UserInfoEntity userInfoEntity=userInfoDao.getUserInfoById(uid);
        if(userInfoEntity==null)
            return null;

        return userInfoEntity.getName();
    }

    @Override
    public void startListening(final SessionListContract.ListenCallback callback) {
        //context.startService(new Intent(context, MessageLoopService.class));

        DatagramListener changedListener=new DatagramListener() {
            @Override
            public void onDatagramReach(Datagram datagram) {
                log.debug("SESSION LIST MESSAGE EXECUTED");
                callback.onDataReach();
            }
        };

        DatagramListener upgradeListener=new DatagramListener() {
            @Override
            public void onDatagramReach(Datagram datagram) {
                String json=datagram.getParamsAsString().get("upgrade_status");
                UpgradeStatus upgradeStatus=JSON.parseObject(json, UpgradeStatus.class);
                callback.onUpgrade(upgradeStatus);
            }
        };

        DatagramListener connectionStatusListener=new DatagramListener() {
            @Override
            public void onDatagramReach(Datagram datagram) {
                int status=Integer.parseInt(datagram.getParamsAsString().get("status"));
                callback.onConnectionStatusChanged(status);
            }
        };

        ActionReportListener authReportListener=new ActionReportListener() {
            @Override
            public void onActionReportReach(ActionReport actionReport) {
                if(!actionReport.getActionStatusInBoolean()){
                    callback.onAuthFailed();
                }
            }
        };

        MessageLoopUtils.registerActionReportListenerNormal("SESSION_LIST_AUTH_REPORT",Datagram.IDENTIFIER_SIGN_IN,authReportListener);

        MessageLoopUtils.registerListenerNormal("SESSION_LIST_AUTH_FAILED", SendDatagramUtils.INBOX_IDENTIFIER_AUTH_INFO_LOST, new DatagramListener() {
            @Override
            public void onDatagramReach(Datagram datagram) {
                callback.onAuthFailed();
            }
        });

        MessageLoopUtils.registerListenerNormal("SESSION_LIST_MESSAGE",Datagram.IDENTIFIER_MESSAGE_DETAIL,changedListener);
        MessageLoopUtils.registerListenerNormal("SESSION_LIST_SESSION",Datagram.IDENTIFIER_SESSION_DETAIL,changedListener);
        MessageLoopUtils.registerListenerNormal("SESSION_LIST_USER_INFO",Datagram.IDENTIFIER_USER_INFO,changedListener);
        MessageLoopUtils.registerListenerNormal("SESSION_LIST_UPDATE",Datagram.IDENTIFIER_UPDATE_RECORD,changedListener);
        MessageLoopUtils.registerListenerNormal("SESSION_LIST_UPGRADE",Datagram.IDENTIFIER_UPGRADE_DETAIL,upgradeListener);

        MessageLoopUtils.registerListenerNormal("SESSION_LIST_CONNECTION_STATUS",SendDatagramUtils.INBOX_IDENTIFIER_CONNECTION_STATUS,connectionStatusListener);
    }

    @Override
    public void stopListening() {
        MessageLoopUtils.unregisterListener("SESSION_LIST_REFRESH_REPORT");
        MessageLoopUtils.unregisterListener("SESSION_LIST_AUTH_REPORT");
        MessageLoopUtils.unregisterListener("SESSION_LIST_SYNC_REPORT");
        MessageLoopUtils.unregisterListener("SESSION_LIST_MESSAGE");
        MessageLoopUtils.unregisterListener("SESSION_LIST_SESSION");
        MessageLoopUtils.unregisterListener("SESSION_LIST_USER_INFO");
        MessageLoopUtils.unregisterListener("SESSION_LIST_UPDATE");
        MessageLoopUtils.unregisterListener("SESSION_LIST_UPGRADE");
        MessageLoopUtils.unregisterListener("SESSION_LIST_CONNECTION_STATUS");
        MessageLoopUtils.unregisterListener("SESSION_LIST_AUTH_FAILED");
    }
}
