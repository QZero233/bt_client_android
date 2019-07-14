package com.nasa.bt.update;

import android.content.Context;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.entity.UpdateEntity;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.loop.MessageLoopResource;

import org.apache.log4j.Logger;

public class UpdateProcessor {

    private static final Logger log= AppLogConfigurator.getLogger();

    public static boolean processUpdate(UpdateEntity updateEntity, Context context){
        int type=updateEntity.getType();
        if(type==UpdateEntity.TYPE_SESSION_CREATE){
            log.debug("会话添加，收到更新");
            String newSessionId=updateEntity.getMore();
            Datagram datagram=new Datagram(Datagram.IDENTIFIER_SESSION_DETAIL,new ParamBuilder().putParam("session_id",newSessionId).build());
            MessageLoopResource.sendDatagram(datagram);
            return true;
        }else if(type==UpdateEntity.TYPE_SESSION_DELETE){
            log.debug("会话被删除，收到更新");
            String deleteSessionId=updateEntity.getMore();
            SessionDao sessionDao=new SessionDao(context);
            return sessionDao.setSessionDisabled(deleteSessionId);
        }

        //未知类型，返回处理失败
        return false;
    }

}
