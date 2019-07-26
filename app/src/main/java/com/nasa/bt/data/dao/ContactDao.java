package com.nasa.bt.data.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.data.entity.ContactEntity;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;

import java.util.List;

public class ContactDao {

    private static final Logger log= AppLogConfigurator.getLogger();
    private Dao<ContactEntity,String> dao;
    private Context context;

    public ContactDao(Context context) {
        this.context = context;
        try {
            dao= LocalDatabaseHelper.getInstance(context).getDao(ContactEntity.class);
        }catch (Exception e){
            log.error("在获取ContactDao时错误",e);
        }
    }

    public List<ContactEntity> getAllContacts(){
        try {
            return dao.queryForAll();
        }catch (Exception e){
            log.error("在获取所有联系人信息时异常",e);
            return null;
        }
    }

    public boolean addContact(ContactEntity contactEntity){
        try {
            return dao.createOrUpdate(contactEntity).getNumLinesChanged()==1;
        }catch (Exception e){
            log.error("在添加联系人时异常",e);
            return false;
        }
    }

    public boolean deleteContactByUid(String uid){
        try {
            DeleteBuilder deleteBuilder=dao.deleteBuilder();
            deleteBuilder.setWhere(deleteBuilder.where().idEq(uid));
            return deleteBuilder.delete()==1;
        }catch (Exception e){
            log.error("在删除联系人时异常",e);
            return false;
        }
    }

}
