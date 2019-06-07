package com.nasa.bt.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.nasa.bt.annotations.ClassVerCode;
import com.nasa.bt.annotations.MainKey;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class CommonDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "GreenApp-CommonDbHelper";
    private Class objClass;
    private Context context;
    private String nameParam="";

    public String getParam(){
        return nameParam;
    }

    private static String getDbNameFromClass(Class objClass,String param) {
        String name = objClass.getName();
        String[] nameArray = name.split("\\.");
        name = nameArray[nameArray.length - 1];
        name.toLowerCase();
        name+=param;
        return name;
    }

    private static String getTabNameFromClass(Class objClass) {
        String name = objClass.getName();
        String[] nameArray = name.split("\\.");
        name = nameArray[nameArray.length - 1];
        name = name.toLowerCase();
        return name;
    }

    private static int getVerCodeFromClass(Class objClass) {
        ClassVerCode code = (ClassVerCode) objClass.getAnnotation(ClassVerCode.class);
        if (code == null)
            return 1;
        else
            return code.value();
    }

    private static String getSqlTypeByClass(Class type) {
        if (type == String.class)
            return "VARCHAR";
        else if (type == int.class)
            return "INT";
        else if (type == short.class)
            return "SHORT";
        else if (type == long.class)
            return "LONG";
        else if (type == double.class)
            return "DOUBLE";
        else if (type == float.class)
            return "FLOAT";
        else if(type==boolean.class)
            return "BOOLEAN";
        else
            return null;
    }

    public CommonDbHelper(Context context, Class objClass,String param) {
        super(context, getDbNameFromClass(objClass,param), null, getVerCodeFromClass(objClass));
        this.objClass = objClass;
        this.context = context;
    }

    private static void createTable(Class objClass,SQLiteDatabase db) {
        String tabName = getTabNameFromClass(objClass);
        StringBuffer sql = new StringBuffer();
        sql.append("CREATE TABLE ");
        sql.append(tabName);
        sql.append("(");

        Field[] fields = objClass.getDeclaredFields();
        for (Field field : fields) {
            int mob = field.getModifiers();
            if (Modifier.isFinal(mob) || Modifier.isStatic(mob))
                continue;

            Class type = field.getType();
            String sqlType = getSqlTypeByClass(type);
            if (sqlType == null)
                continue;

            String name = field.getName();
            sql.append(name);
            sql.append(" ");
            sql.append(sqlType);
            sql.append(",");
        }

        if (sql.charAt(sql.length() - 1) == ',')
            sql.deleteCharAt(sql.length() - 1);
        sql.append(")");

        Log.i(TAG, "Create table sql::" + sql.toString());
        db.execSQL(sql.toString());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(objClass,db);
    }

    private static void updateTable(Class objClass,SQLiteDatabase db) {
        String tabName = getTabNameFromClass(objClass);

        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tabName + ")", null);
        if (!cursor.moveToFirst())
            return;

        List<String> columns = new ArrayList<>();

        do {
            String name = cursor.getString(cursor.getColumnIndex("name"));
            Log.i(TAG, "columnName::" + name);
            columns.add(name);
        } while (cursor.moveToNext());
        cursor.close();

        Field[] fields = objClass.getDeclaredFields();

        for (Field field : fields) {
            int mod=field.getModifiers();
            if(Modifier.isStatic(mod) || Modifier.isFinal(mod))
                continue;

            boolean found = false;
            for (String column : columns) {
                //Ok,pass
                if (field.getName().equals(column)) {
                    found = true;
                    break;
                }
            }
            if (found)
                continue;
            //Can't find this field in table,so add
            String sql = "ALTER TABLE " + tabName + " ADD " + field.getName() + " " + getSqlTypeByClass(field.getType());
            Log.i(TAG, "Add column sql::" + sql);
            db.execSQL(sql);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateTable(objClass,db);
    }

    public long insert(Object obj){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues values= getValuesByObject(obj,true);
        return db.insert(getTabNameFromClass(objClass),null,values);
    }

    private static ContentValues getValuesByObject(Object obj,boolean isInsert){
        ContentValues values=new ContentValues();

        Class objClass =obj.getClass();
        Field[] fields= objClass.getDeclaredFields();

        for(Field field:fields){

            if(!isInsert && field.getAnnotation(MainKey.class)!=null)
                continue;

            field.setAccessible(true);
            String name=field.getName();

            try{
                int mod=field.getModifiers();
                if(Modifier.isStatic(mod) || Modifier.isFinal(mod))
                    continue;
                Class type=field.getType();
                if(type==String.class)
                    values.put(name,field.get(obj).toString());
                else if(type==int.class)
                    values.put(name,field.getInt(obj));
                else if(type==byte.class)
                    values.put(name,field.getByte(obj));
                else if(type==short.class)
                    values.put(name,field.getShort(obj));
                else if(type==long.class)
                    values.put(name,field.getLong(obj));
                else if(type==float.class)
                    values.put(name,field.getFloat(obj));
                else if(type==double.class)
                    values.put(name,field.getDouble(obj));
                else if(type==char.class)
                    values.put(name,field.getChar(obj)+"");
                else if(type==boolean.class)
                    values.put(name,field.getBoolean(obj));
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return values;
    }

    private static String getWhereByObject(Object obj){
        Field[] fields=obj.getClass().getDeclaredFields();

        StringBuffer sb=new StringBuffer();
        for(Field field:fields){
            if(field.getAnnotation(MainKey.class)==null)
                continue;
            field.setAccessible(true);

            String where=field.getName()+"=";
            Class type=field.getType();

            try{
                if(type==int.class || type==long.class || type==short.class || type==float.class || type==double.class || type==boolean.class)
                    where+=String.valueOf(field.get(obj));
                else
                    where+="'"+field.get(obj).toString()+"'";

                if(String.valueOf(field.get(obj)).equals("0"))
                    throw new Exception();

                where+=" and ";
            }catch (Exception e){
                continue;
            }
            sb.append(where);
        }

        if(sb.length()>2 && sb.charAt(sb.length()-2)=='d')
            sb.delete(sb.length()-5,sb.length()-1);
        return sb.toString();
    }

    public long delete(Object obj){
        String where="";
        if(obj!=null)
            where=getWhereByObject(obj);
        Log.i(TAG,"delete where sql::"+where);
        SQLiteDatabase db=getWritableDatabase();
        return db.delete(getTabNameFromClass(objClass),where,null);
    }

    public long update(Object oldObj, Object newObj){
        String where=getWhereByObject(oldObj);
        Log.i(TAG,"update where sql::"+where);
        ContentValues values=getValuesByObject(newObj,false);

        SQLiteDatabase db=getWritableDatabase();
        return db.update(getTabNameFromClass(objClass),values,where,null);
    }

    private List  getObjectsByCursor(Cursor cursor){
        if(!cursor.moveToFirst())
            return new ArrayList();

        List result=new ArrayList();

        Field[] fields=objClass.getDeclaredFields();

        try{
            do{
                Object obj=objClass.newInstance();
                for(Field field:fields){
                    int mod=field.getModifiers();
                    if(Modifier.isStatic(mod) || Modifier.isFinal(mod))
                        continue;

                    field.setAccessible(true);
                    String name=field.getName();
                    Class type=field.getType();

                    int index=cursor.getColumnIndex(name);

                    if(type==int.class)
                        field.setInt(obj,cursor.getInt(index));
                    else if(type==long.class)
                        field.setLong(obj,cursor.getLong(index));
                    else if(type==short.class)
                        field.setShort(obj,cursor.getShort(index));
                    else if(type==float.class)
                        field.setFloat(obj,cursor.getFloat(index));
                    else if(type==double.class)
                        field.setDouble(obj,cursor.getDouble(index));
                    else if(type==String.class)
                        field.set(obj,cursor.getString(index));
                    else if(type==boolean.class)
                        field.setBoolean(obj,(cursor.getInt(index)==1)?true:false);
                }
                result.add(obj);
            }while (cursor.moveToNext());
        }catch (Exception e){
            e.printStackTrace();
        }
        return  result;
    }

    public List query(){
        SQLiteDatabase db=getReadableDatabase();
        Cursor cursor= db.query(getTabNameFromClass(objClass),null,null,null,null,null,null);
        return getObjectsByCursor(cursor);
    }

    public List queryOrder(String by){
        SQLiteDatabase db=getReadableDatabase();
        Cursor cursor= db.query(getTabNameFromClass(objClass),null,null,null,null,null,by);
        return getObjectsByCursor(cursor);
    }

    public List query(Object obj){
        String where=getWhereByObject(obj);
        Log.i(TAG,"query where sql::"+where);

        SQLiteDatabase db=getReadableDatabase();
        Cursor cursor= db.query(getTabNameFromClass(objClass),null,where,null,null,null,null);
        return getObjectsByCursor(cursor);
    }

    public List query(String sql){
        Log.i("GreenApp",sql);
        SQLiteDatabase db=getReadableDatabase();
        Cursor cursor=db.rawQuery(sql,null);

        return getObjectsByCursor(cursor);
    }

    public Object querySingle(String sql){
        List<Object> list=query(sql);
        if(list==null || list.isEmpty())
            return null;
        return list.get(0);
    }

    public void execSql(String sql){
        SQLiteDatabase db=getWritableDatabase();
        db.execSQL(sql);
    }

    public void testQuery(){
        SQLiteDatabase db=getReadableDatabase();
        db.rawQuery("SELECT * FROM "+getTabNameFromClass(objClass),null);
    }
}
