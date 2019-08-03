package com.nasa.bt.update;

import android.content.Context;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.entity.UpdateRecordEntity;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.loop.SendDatagramUtils;

import org.apache.log4j.Logger;

public class UpdateProcessor {

    private static final Logger log= AppLogConfigurator.getLogger();

        public static void processUpdate(UpdateRecordEntity updateRecordEntity, Context context){
        int type=updateRecordEntity.getEntityType();
        if(type==UpdateRecordEntity.TYPE_SESSION){
            String sessionId=updateRecordEntity.getEntityId();
            if(updateRecordEntity.getEntityStatus()==UpdateRecordEntity.STATUS_DELETED){
                //会话被删除
                log.debug("会话被删除，会话id="+sessionId);
                SessionDao sessionDao=new SessionDao(context);
                sessionDao.setSessionDisabled(sessionId);
            }else{
                //会话被更改
                log.debug("收到一个会话更新，会话id="+sessionId);
                Datagram datagram=new Datagram(Datagram.IDENTIFIER_SESSION_DETAIL,new ParamBuilder().putParam("session_id",sessionId).build());
                SendDatagramUtils.sendDatagram(datagram);
            }


        }
    }

}
