package com.nasa.bt.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.nasa.bt.data.entity.ContactEntity;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.utils.LocalSettingsUtils;

import org.apache.log4j.Logger;

public class LocalDatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String LOCAL_DB_NAME_BEGIN="appData";
    private static final int CURRENT_VER_CODE=3;

    private static final Logger log= AppLogConfigurator.getLogger();

    private static LocalDatabaseHelper instance;

    private static String getDatabaseName(Context context){
        return LOCAL_DB_NAME_BEGIN+"-"+ LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_NAME)+"-"+LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_SERVER_IP);
    }

    private LocalDatabaseHelper(Context context,String name) {
        super(context, name, null, CURRENT_VER_CODE);
    }

    public static LocalDatabaseHelper getInstance(Context context){
        if(instance==null){
            synchronized (LocalDatabaseHelper.class){
                if(instance==null)
                    instance=new LocalDatabaseHelper(context,getDatabaseName(context));
            }
        }

        return instance;
    }

    public static void reset(Context context){
        instance=null;
        synchronized (LocalDatabaseHelper.class){
            if(instance==null)
                instance=new LocalDatabaseHelper(context,getDatabaseName(context));
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, MessageEntity.class);
            TableUtils.createTableIfNotExists(connectionSource, SessionEntity.class);
            TableUtils.createTableIfNotExists(connectionSource, UserInfoEntity.class);
            TableUtils.createTableIfNotExists(connectionSource, ContactEntity.class);
        }catch (Exception e){
            log.error("使用ORM建表时错误",e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int oldVer, int newVer) {
        if(oldVer==1){
            /**
             * 1.Session里面添加了 disabled字段
             */
            try {
                Dao sessionDao=getDao(SessionEntity.class);
                sessionDao.executeRaw("ALTER TABLE session ADD COLUMN disabled boolean");
            }catch (Exception e){
                log.error("升级版本 1—>2 时异常",e);
            }
        }else if(oldVer==2){
            /**
             * 1.增加了 ContactEntity 这个实体类
             */
            try {
                TableUtils.createTableIfNotExists(connectionSource,ContactEntity.class);
            }catch (Exception e){
                log.error("升级版本 2—>3 时异常",e);
            }
        }
    }
}
