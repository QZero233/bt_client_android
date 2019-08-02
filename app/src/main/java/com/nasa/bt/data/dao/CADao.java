package com.nasa.bt.data.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.data.entity.TrustedCAPublicKeyEntity;
import com.nasa.bt.data.entity.TrustedRemotePublicKeyEntity;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;

import java.util.List;

public class CADao {

    private static final Logger log= AppLogConfigurator.getLogger();
    private Dao<TrustedRemotePublicKeyEntity,String> remoteKeyDao;
    private Dao<TrustedCAPublicKeyEntity,String> keyDao;
    private Context context;

    public CADao(Context context) {
        this.context = context;
        try {
            remoteKeyDao =LocalDatabaseHelper.getInstance(context).getDao(TrustedRemotePublicKeyEntity.class);
            keyDao=LocalDatabaseHelper.getInstance(context).getDao(TrustedCAPublicKeyEntity.class);
        }catch (Exception e){
            log.error("在获取CADao时错误",e);
        }
    }

    public boolean addTrustedRemoteKey(TrustedRemotePublicKeyEntity trustedRemotePublicKeyEntity){
        try{
            return remoteKeyDao.createOrUpdate(trustedRemotePublicKeyEntity).getNumLinesChanged()==1;
        }catch (Exception e){
            log.error("添加信任的远程公钥时异常",e);
            return false;
        }
    }

    public TrustedRemotePublicKeyEntity getTrustedRemoteKey(String ip){
        try {
            TrustedRemotePublicKeyEntity trustedCAEntity= remoteKeyDao.queryForId(ip);
            return trustedCAEntity;
        }catch (Exception e){
            log.error("获取信任的远程公钥时异常,ip="+ip,e);
            return null;
        }
    }

    public List<TrustedRemotePublicKeyEntity> getAllTrustedRemoteKeys(){
        try {
            return remoteKeyDao.queryForAll();
        }catch (Exception e){
            log.error("在获取全部信任的远程公钥时异常",e);
            return null;
        }
    }

    public boolean deleteTrustedRemoteKey(String ip){
        try {
            return remoteKeyDao.deleteById(ip)==1;
        }catch (Exception e){
            log.error("删除信任的远程公钥时异常,ip="+ip,e);
            return false;
        }
    }



}
