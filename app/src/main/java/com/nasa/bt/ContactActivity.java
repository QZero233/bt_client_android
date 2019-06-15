package com.nasa.bt;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import com.nasa.bt.cls.Contact;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.utils.CommonDbHelper;
import com.nasa.bt.utils.LocalDbUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {

    private SearchView sv_name;
    private ListView lv_contact;
    private CommonDbHelper contactHelper;
    private List<Contact> contactList;
    private ProgressBar pb;

    private Handler userInfoHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            pb.setVisibility(View.GONE);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();

            if(params.get("exist").equalsIgnoreCase("0")){
                Toast.makeText(ContactActivity.this,"用户不存在",Toast.LENGTH_SHORT).show();
                return;
            }

            String uid=params.get("uid");
            String name=params.get("name");
            Contact contact=new Contact(uid,name);
            contactHelper.execSql("DELETE FROM contact WHERE name='"+name+"'");
            contactHelper.insert(contact);
            Toast.makeText(ContactActivity.this,"搜索添加成功",Toast.LENGTH_SHORT).show();
            reload();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        sv_name=findViewById(R.id.sv_name);
        lv_contact=findViewById(R.id.lv_contact);
        pb=findViewById(R.id.pb);
        contactHelper= LocalDbUtils.getContactHelper(this);

        sv_name.setOnQueryTextListener(this);
        lv_contact.setOnItemClickListener(this);
        reload();

        MessageIntent intent=new MessageIntent("CONTACT_USER_INFO",Datagram.IDENTIFIER_RETURN_USER_INFO,userInfoHandler,0,1);
        MessageLoop.addIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageLoop.removeIntent(Datagram.IDENTIFIER_RETURN_USER_INFO,"CONTACT_USER_INFO",1);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        if(TextUtils.isEmpty(s)){
            Toast.makeText(this,"不能为空",Toast.LENGTH_SHORT).show();
            return false;
        }

        Map<String,byte[]> params=new HashMap<>();
        params.put("name",s.getBytes());
        Datagram datagramQuery=new Datagram(Datagram.IDENTIFIER_GET_USER_INFO,params);
        LoopResource.sendDatagram(datagramQuery);
        pb.setVisibility(View.VISIBLE);
        return true;
    }

    private void reload(){
        contactList=contactHelper.query();
        if(contactList==null)
            contactList=new ArrayList<>();
        lv_contact.setAdapter(new ShowContactAdapter(contactList,this));
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Contact contact=contactList.get(i);

    }
}

class ShowContactAdapter extends BaseAdapter{

    private List<Contact> contactList;
    private Context context;

    public ShowContactAdapter(List<Contact> contactList, Context context) {
        this.contactList = contactList;
        this.context = context;
    }

    @Override
    public int getCount() {
        if(contactList==null || contactList.isEmpty())
            return 0;
        return contactList.size();
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
        tv.setText(contactList.get(i).getName());
        tv.setTextSize(20);

        return tv;
    }
}