package com.nasa.bt.data.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedUpdate;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.utils.LocalSettingsUtils;

import org.apache.log4j.Logger;

import java.util.List;

public class MessageDao {

    private static final Logger log= AppLogConfigurator.getLogger();
    private Dao<MessageEntity,String> dao;
    private Context context;

    public MessageDao(Context context) {
        this.context = context;
        try {
            dao= LocalDatabaseHelper.getInstance(context).getDao(MessageEntity.class);
        }catch (Exception e){
            log.error("在获取MessageDao时错误",e);
        }
    }

    public boolean deleteAllMessage(String dstUid){
        try {
            DeleteBuilder deleteBuilder=dao.deleteBuilder();
            deleteBuilder.setWhere(deleteBuilder.where().eq("dstUid",dstUid).or().eq("srcUid",dstUid));
            return deleteBuilder.delete()!=-1;
        }catch (Exception e){
            log.error("清空消息时异常，dstUid="+dstUid,e);
            return false;
        }
    }

    public List<MessageEntity> getUnreadMessageBySessionId(String sessionId){
        try{
            QueryBuilder queryBuilder=dao.queryBuilder();
            queryBuilder.setWhere(queryBuilder.where().eq("sessionId",sessionId).and().
                    eq("status",MessageEntity.STATUS_UNREAD).and().ne("srcUid", LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_UID)));
            return queryBuilder.query();
        }catch (Exception e){
            log.error("获取未读消息时异常,sessionId="+sessionId,e);
            return null;
        }
    }



    public List<MessageEntity> getAllMessage(String sessionId){
        try {
            QueryBuilder queryBuilder=dao.queryBuilder();
            queryBuilder.setWhere(queryBuilder.where().eq("sessionId",sessionId));
            queryBuilder.orderBy("time",true);
            return queryBuilder.query();
        }catch (Exception e){
            log.error("获取所有消息时异常",e);
            return null;
        }
    }

    public void addMessage(MessageEntity messageEntity){
        try {
            dao.createIfNotExists(messageEntity);
        }catch (Exception e){
            log.error("插入消息时异常",e);
        }
    }

    public boolean changeMessageStatusById(String msgId,int status){
        try {
            UpdateBuilder updateBuilder=dao.updateBuilder();
            updateBuilder.setWhere(updateBuilder.where().idEq(msgId));
            updateBuilder.updateColumnValue("status",status);
            return updateBuilder.update()==1;
        }catch (Exception e){
            log.error("标记信息已读时异常",e);
            return false;
        }
    }

    public boolean markReadById(String msgId){
        return changeMessageStatusById(msgId,MessageEntity.STATUS_READ);
    }

}
