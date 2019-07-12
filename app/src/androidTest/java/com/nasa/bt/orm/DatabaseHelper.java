package com.nasa.bt.orm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static DatabaseHelper instance;

    private DatabaseHelper(Context context) {
        super(context, "testDb", null, 2);
    }

    public static DatabaseHelper getInstance(Context context){
        if(instance==null){
            synchronized (DatabaseHelper.class){
                if(instance==null)
                    instance=new DatabaseHelper(context);
            }
        }

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource,Student.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int i, int i1) {
        try{
            /**
             * 不采用下面的方式
             * TableUtils.dropTable(connectionSource,Student.class,false);
             * TableUtils.createTable(connectionSource,Student.class);
             */

            /**
             * 根据版本号手动进行相应操作
             */

            if(i1==3){
                getDao(Student.class).executeRaw("ALTER TABLE student ADD COLUMN ");
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
