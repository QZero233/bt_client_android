package com.nasa.bt.data.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;

import java.util.List;

public class SessionDao {

    private static final Logger log= AppLogConfigurator.getLogger();
    private Dao<SessionEntity,String> dao;
    private Context context;

    public SessionDao(Context context) {
        this.context = context;
        try {
            dao= LocalDatabaseHelper.getInstance(context).getDao(SessionEntity.class);
        }catch (Exception e){
            log.error("在获取UserInfoDao时错误",e);
        }
    }

    public boolean deleteSession(String sessionId){
        try {
            return dao.deleteById(sessionId)==1;
        }catch (Exception e){
            log.error("删除会话时异常,sessionId="+sessionId);
            return false;
        }
    }

    public SessionEntity getSessionById(String sessionId){
        try {
            QueryBuilder queryBuilder=dao.queryBuilder();
            queryBuilder.setWhere(queryBuilder.where().idEq(sessionId));
            return (SessionEntity) queryBuilder.queryForFirst();
        }catch (Exception e){
            log.error("根据ID获取Session时异常",e);
            return null;
        }
    }

    public boolean changeLastStatus(String sessionId,String lastMessage,long lastTime){
        try {
            UpdateBuilder updateBuilder=dao.updateBuilder();
            updateBuilder.setWhere(updateBuilder.where().idEq(sessionId));
            updateBuilder.updateColumnValue("lastMessage",lastMessage);
            updateBuilder.updateColumnValue("lastTime",lastTime);
            return updateBuilder.update()==1;
        }catch (Exception e){
            log.error("更改最后一次状态时异常",e);
            return false;
        }
    }

    public List<SessionEntity> getAllSession(){
        try {
            return dao.queryBuilder().orderBy("lastTime",false).query();
        }catch (Exception e){
            log.error("获取全部会话信息时异常",e);
            return null;
        }
    }

    public void addOrUpdateSession(SessionEntity sessionEntity){
        try{
            SessionEntity sessionEntityExists=getSessionById(sessionEntity.getSessionId());
            if(sessionEntityExists!=null){
                UpdateBuilder updateBuilder=dao.updateBuilder();
                updateBuilder.setWhere(updateBuilder.where().idEq(sessionEntity.getSessionId()));
                updateBuilder.updateColumnValue("params",sessionEntity.getParams());
                updateBuilder.update();
            }else{
                dao.create(sessionEntity);
            }
        }catch (Exception e){
            log.error("添加会话时异常",e);
        }
    }

    public boolean setSessionDisabled(String sessionId){
        try {
            UpdateBuilder updateBuilder=dao.updateBuilder();
            updateBuilder.setWhere(updateBuilder.where().idEq(sessionId));
            updateBuilder.updateColumnValue("disabled",true);
            return updateBuilder.update()==1;
        }catch (Exception e){
            log.error("在把会话设置成已删除时异常，sessionId="+sessionId);
            return false;
        }
    }

}
