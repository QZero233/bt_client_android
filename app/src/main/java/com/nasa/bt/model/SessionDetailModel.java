package com.nasa.bt.model;

import android.content.Context;
import android.text.TextUtils;

import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.contract.SessionDetailContract;
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.loop.ActionReportListener;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

import java.util.Map;

public class SessionDetailModel implements SessionDetailContract.Model {

    private Context context;

    private SessionDao sessionDao;

    public SessionDetailModel(Context context) {
        this.context = context;

        sessionDao=new SessionDao(context);
    }

    @Override
    public void closeSession(final String sessionId, final SessionDetailContract.Callback callback) {
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_DELETE_SESSION, new ParamBuilder().putParam("session_id",sessionId).build());
        SendDatagramUtils.sendDatagram(datagram);

        MessageLoopUtils.registerSpecifiedTimesActionReportListener("SESSION_DETAIL_DELETE_REPORT", Datagram.IDENTIFIER_DELETE_SESSION, 1, new ActionReportListener() {
            @Override
            public void onActionReportReach(ActionReport actionReport) {
                if(actionReport.getActionStatusInBoolean()){
                    sessionDao.setSessionDisabled(sessionId);
                    callback.onSuccess();
                }else{
                    callback.onFailure(0);
                }
            }
        });
    }

    @Override
    public void updateRemarks(final SessionEntity sessionEntity, String newRemarks, final SessionDetailContract.Callback callback) {
        Map<String,String> params=sessionEntity.getParamsInMap();
        params.put("remarks",newRemarks);
        sessionEntity.setParamsInMap(params);

        Datagram datagram=new Datagram(Datagram.IDENTIFIER_UPDATE_SESSION,new ParamBuilder().putParam("session_id",sessionEntity.getSessionId()).
                putParam("params",sessionEntity.getParams()).build());
        SendDatagramUtils.sendDatagram(datagram);

        MessageLoopUtils.registerSpecifiedTimesActionReportListener("SESSION_DETAIL_UPDATE_REPORT",Datagram.IDENTIFIER_UPDATE_SESSION, 1, new ActionReportListener() {
            @Override
            public void onActionReportReach(ActionReport actionReport) {
                if(actionReport.getActionStatusInBoolean()){
                    sessionDao.addOrUpdateSession(sessionEntity);
                    callback.onSuccess();
                }else{
                    callback.onFailure(0);
                }
            }
        });
    }

    @Override
    public SessionEntity getSessionEntityBySessionId(String sessionId) {
        return sessionDao.getSessionById(sessionId);
    }

    @Override
    public UserInfoEntity getDstUserInfoBySessionEntity(SessionEntity sessionEntity) {
        String uid= LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_UID);
        if(TextUtils.isEmpty(uid) || sessionEntity==null)
            return null;

        UserInfoDao userInfoDao=new UserInfoDao(context);
        String dstUid=sessionEntity.getIdOfOther(uid);

        return userInfoDao.getUserInfoById(dstUid);
    }

    @Override
    public boolean clean(SessionEntity sessionEntity){
        if(sessionEntity==null)
            return false;

        MessageDao messageDao=new MessageDao(context);

        if(sessionEntity.isDisabled()){
            //删除会话+清除消息
            return sessionDao.deleteSession(sessionEntity.getSessionId()) && messageDao.deleteAllMessage(sessionEntity.getSessionId());
        }else{
            //清除消息
            return messageDao.deleteAllMessage(sessionEntity.getSessionId());
        }
    }
}
