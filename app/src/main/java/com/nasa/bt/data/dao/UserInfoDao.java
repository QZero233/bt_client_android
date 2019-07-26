package com.nasa.bt.data.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;

import java.util.List;

public class UserInfoDao {

    private static final Logger log= AppLogConfigurator.getLogger();
    private Dao<UserInfoEntity,String> dao;
    private Context context;

    public UserInfoDao(Context context) {
        this.context = context;
        try {
            dao= LocalDatabaseHelper.getInstance(context).getDao(UserInfoEntity.class);
        }catch (Exception e){
            log.error("在获取UserInfoDao时错误",e);
        }
    }

    public UserInfoEntity getUserInfoById(String id){
        try {
            QueryBuilder queryBuilder=dao.queryBuilder();
            queryBuilder.setWhere(queryBuilder.where().idEq(id));
            return (UserInfoEntity) queryBuilder.queryForFirst();
        }catch (Exception e){
            log.error("根据ID查询用户时异常",e);
            return null;
        }
    }

    public List<UserInfoEntity> getAllUserInfo(){
        try {
            return dao.queryForAll();
        }catch (Exception e){
            log.error("获取全部用户信息时异常",e);
            return null;
        }
    }


    public boolean deleteUserByName(String name){
        try {
            DeleteBuilder deleteBuilder=dao.deleteBuilder();
            deleteBuilder.setWhere(deleteBuilder.where().eq("name",name));
            return deleteBuilder.delete()==1;
        }catch (Exception e){
            log.error("删除用户时异常,name="+name,e);
            return false;
        }
    }

    public boolean addUser(UserInfoEntity userInfoEntity){
        try{
            return dao.createOrUpdate(userInfoEntity).getNumLinesChanged()==1;
        }catch (Exception e){
            log.error("添加用户时异常",e);
            return false;
        }
    }

}
