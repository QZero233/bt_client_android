package com.nasa.bt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.data.dao.ContactDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.ContactEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.loop.DatagramListener;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private SearchView sv_name;
    private ListView lv_contact;
    private ContactDao contactDao;
    private UserInfoDao userInfoDao;
    private List<UserInfoEntity> userInfoEntityList;
    private ProgressBar pb;

    private DatagramListener userInfoListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            pb.setVisibility(View.GONE);
            Map<String,String> params=datagram.getParamsAsString();

            if(params.get("exist").equalsIgnoreCase("0")){
                Toast.makeText(ContactActivity.this,"用户不存在",Toast.LENGTH_SHORT).show();
                return;
            }else{
                ContactEntity contactEntity=new ContactEntity(params.get("uid"));
                if(contactDao.addContact(contactEntity)){
                    Toast.makeText(ContactActivity.this,"搜索添加成功",Toast.LENGTH_SHORT).show();
                    reload();
                }else{
                    Toast.makeText(ContactActivity.this,"添加失败",Toast.LENGTH_SHORT).show();
                }
            }


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        sv_name=findViewById(R.id.sv_name);
        lv_contact=findViewById(R.id.lv_contact);
        pb=findViewById(R.id.pb);

        userInfoDao=new UserInfoDao(this);
        contactDao=new ContactDao(this);

        sv_name.setOnQueryTextListener(this);
        lv_contact.setOnItemClickListener(this);
        lv_contact.setOnItemLongClickListener(this);
        reload();

        MessageLoopUtils.registerListenerNormal("CONTACT_USER_INFO",Datagram.IDENTIFIER_USER_INFO,userInfoListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageLoopUtils.unregisterListener("CONTACT_USER_INFO");
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        if(TextUtils.isEmpty(s)){
            Toast.makeText(this,"不能为空",Toast.LENGTH_SHORT).show();
            return false;
        }

        if(s.equals(LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_NAME))){
            Toast.makeText(this,"亲，请不要没事和自己对话",Toast.LENGTH_SHORT).show();
            return false;
        }

        Map<String,byte[]> params=new HashMap<>();
        params.put("name",s.getBytes());
        Datagram datagramQuery=new Datagram(Datagram.IDENTIFIER_USER_INFO,params);
        SendDatagramUtils.sendDatagram(datagramQuery);
        pb.setVisibility(View.VISIBLE);
        return true;
    }

    private void reload(){
        List<ContactEntity> contactEntityList=contactDao.getAllContacts();
        userInfoEntityList=new ArrayList<>();
        if(contactEntityList!=null){
            for(ContactEntity contactEntity:contactEntityList){
                UserInfoEntity userInfoEntity=userInfoDao.getUserInfoById(contactEntity.getDstUid());
                userInfoEntityList.add(userInfoEntity);
            }
        }
        lv_contact.setAdapter(new ShowContactAdapter(userInfoEntityList,this));
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        UserInfoEntity user= userInfoEntityList.get(i);
        Intent intent=new Intent(this,UserDetailActivity.class);
        intent.putExtra("user",user);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        final UserInfoEntity userInfoEntity = userInfoEntityList.get(i);
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setMessage("是否删除联系人 "+ userInfoEntity.getName()+" （删除后会保留本地聊天记录）");
        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                contactDao.deleteContactByUid(userInfoEntity.getId());
                reload();
                Toast.makeText(ContactActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
        return true;
    }
}

class ShowContactAdapter extends BaseAdapter{

    private List<UserInfoEntity> userList;
    private Context context;

    public ShowContactAdapter(List<UserInfoEntity> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    @Override
    public int getCount() {
        if(userList ==null || userList.isEmpty())
            return 0;
        return userList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView tv=new TextView(context);
        tv.setText(userList.get(i).getName());
        tv.setTextSize(20);

        return tv;
    }
}