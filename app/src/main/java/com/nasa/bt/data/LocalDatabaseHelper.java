package com.nasa.bt.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.utils.LocalSettingsUtils;

import org.apache.log4j.Logger;

public class LocalDatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String LOCAL_DB_NAME_BEGIN="appData";
    private static final int CURRENT_VER_CODE=1;

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

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, MessageEntity.class);
            TableUtils.createTableIfNotExists(connectionSource, SessionEntity.class);
            TableUtils.createTableIfNotExists(connectionSource, UserInfoEntity.class);
        }catch (Exception e){
            log.error("使用ORM建表时错误",e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int oldVer, int newVer) {
        if(oldVer==1){
            //TODO 数据结构更新操作
        }
    }
}
