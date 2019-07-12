package com.nasa.bt.orm;

import android.support.test.InstrumentationRegistry;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.utils.UUIDUtils;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class OrmLiteTest {

    private DatabaseHelper databaseHelper;
    private Dao<Student,String> dao;
    private static final Logger log= AppLogConfigurator.getLogger();

    @Before
    public void init() throws Exception{
        databaseHelper=DatabaseHelper.getInstance(InstrumentationRegistry.getTargetContext());
        dao=databaseHelper.getDao(Student.class);
    }

    @Test
    public void testInsert() throws Exception{
        Student student=new Student(UUIDUtils.getRandomUUID(),"Alice",16,System.currentTimeMillis());
        log.debug("插入结果 "+dao.create(student));
    }

    @Test
    public void testRead() throws Exception{
        QueryBuilder queryBuilder=dao.queryBuilder();
        queryBuilder.setWhere(queryBuilder.where().eq("name","Alice"));
        Student student= (Student) queryBuilder.queryForFirst();
        log.debug("指定查询结果 "+student);
        log.debug("全部查询结果 "+dao.queryBuilder().query());
    }

    @Test
    public void testUpdate() throws Exception{
        Student alice=new Student("af5a9b87-fee9-45ed-be8f-47f4dba9dc39","Alice",20,System.currentTimeMillis());
        Student bob=new Student("b1c19b87-fee9-45ed-be8f-47f4dba9dc39","Bob",18,System.currentTimeMillis());

        log.debug("Alice操作结果 "+dao.createOrUpdate(alice).getNumLinesChanged());
        log.debug("Bob操作结果 "+dao.createOrUpdate(bob).getNumLinesChanged());
    }

    @Test
    public void testDelete() throws Exception{
        dao.deleteById("b1c19b87-fee9-45ed-be8f-47f4dba9dc39");
    }

}
