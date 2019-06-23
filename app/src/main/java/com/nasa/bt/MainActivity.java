package com.nasa.bt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.utils.CommonDbHelper;
import com.nasa.bt.utils.LocalDbUtils;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.TimeUtils;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {


    private Handler changeHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            reloadUserInfo();
            if(sl_main.isRefreshing()){
                sl_main.setRefreshing(false);
                Toast.makeText(MainActivity.this,"刷新成功",Toast.LENGTH_SHORT).show();
            }
        }
    };


    private ListView lv_users;
    private SwipeRefreshLayout sl_main;

    private List<UserInfo> users;

    MessageIntent intentMessage=new MessageIntent("MAIN_MESSAGE", Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,changeHandler,0,1);
    MessageIntent intentMessageIndex=new MessageIntent("MAIN_MESSAGE_INDEX", Datagram.IDENTIFIER_RETURN_MESSAGE_INDEX,changeHandler,0,1);
    MessageIntent intentUserInfo=new MessageIntent("MAIN_USER_INFO", Datagram.IDENTIFIER_RETURN_USER_INFO,changeHandler,0,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MessageLoop.addIntent(intentMessage);
        MessageLoop.addIntent(intentMessageIndex);
        MessageLoop.addIntent(intentUserInfo);

        KeyUtils.initContext(this);

        String name,code;
        name=LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_NAME);
        code=LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_CODE_HASH);
        if(TextUtils.isEmpty(name) || TextUtils.isEmpty(code)){
            startActivity(new Intent(this,AuthInfoActivity.class));
            Toast.makeText(this,"请设置基本信息",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        startService(new Intent(this, MessageLoopService.class));

        lv_users=findViewById(R.id.lv_users);
        sl_main=findViewById(R.id.sl_main);

        sl_main.setOnRefreshListener(this);
        lv_users.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent=new Intent(MainActivity.this,ChatActivity.class);
                intent.putExtra("userDst",users.get(i));
                startActivity(intent);
            }
        });
        reloadUserInfo();
        setTitle("BugTelegram NASA内测版");
    }

    private void reloadUserInfo(){
        users=new ArrayList<>();
        List<UserInfo> userInfoList=LocalDbUtils.getUserInfoHelper(this).query();
        if(userInfoList!=null){
            for(UserInfo user:userInfoList){
                if(user.getName().equals(LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_NAME)))
                    continue;
                users.add(new UserInfo(user.getName(),user.getId()));
            }
        }
        lv_users.setAdapter(new MainUserAdapter(users,this));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        reloadUserInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.m_settings){
            AlertDialog.Builder builder=new AlertDialog.Builder(this);

            final EditText et_ip=new EditText(this);
            String ip = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_SERVER_IP);
            if (TextUtils.isEmpty(ip))
                ip = MessageLoopService.SERVER_IP_DEFAULT;
            et_ip.setText(ip);

            builder.setView(et_ip);
            builder.setMessage("请输入服务器IP");
            builder.setNegativeButton("取消",null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String newIp=et_ip.getText().toString();
                    LocalSettingsUtils.save(MainActivity.this,LocalSettingsUtils.FIELD_SERVER_IP,newIp);
                    Toast.makeText(MainActivity.this,"修改成功",Toast.LENGTH_SHORT).show();

                    Datagram datagram=new Datagram(LoopResource.INBOX_IDENTIFIER_RECONNECT,null);
                    MessageLoop.processDatagram(datagram);

                    finish();
                }
            });
            builder.show();
        }else if(item.getItemId()==R.id.m_reset_key){
            try {
                KeyUtils utils=KeyUtils.getInstance();
                utils.genKeySet();
                utils.saveKeySet();
                Toast.makeText(this,"重置成功",Toast.LENGTH_SHORT).show();
                finish();
            }catch (Exception e){
                Toast.makeText(this,"重置失败",Toast.LENGTH_SHORT).show();
            }
        }else if(item.getItemId()==R.id.m_contact){
            startActivity(new Intent(this,ContactActivity.class));
        }else if(item.getItemId()==R.id.m_quit){
            LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_NAME,"");
            LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_CODE_HASH,"");
            LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_CODE_LAST,"");
            Datagram datagramDisconnect=new Datagram(LoopResource.INBOX_IDENTIFIER_DISCONNECTED,null);
            MessageLoop.processDatagram(datagramDisconnect);
            Toast.makeText(this,"退出成功",Toast.LENGTH_SHORT).show();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_GET_MESSAGE_INDEX,null);
        LoopResource.sendDatagram(datagram);

        //刷新用户状态
        if(users!=null){
            for(UserInfo userInfo:users){
                Map<String,String> params=new HashMap<>();
                params.put("uid",userInfo.getId());
                Datagram datagramUser=new Datagram(Datagram.IDENTIFIER_GET_USER_INFO,params,"");
                LoopResource.sendDatagram(datagramUser);
            }
        }
    }
}

class MainUserAdapter extends BaseAdapter{

    private List<UserInfo> users;
    private Context context;
    private CommonDbHelper msgHelper;

    public MainUserAdapter(List<UserInfo> users, Context context) {
        this.users = users;
        this.context = context;
        msgHelper=LocalDbUtils.getMsgHelper(context);
    }

    @Override
    public int getCount() {
        if(users==null || users.isEmpty())
            return 0;
        return users.size();
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
        UserInfo user=users.get(i);

        View v=View.inflate(context,R.layout.view_main_user,null);

        TextView tv_name=v.findViewById(R.id.tv_name);
        TextView tv_msg=v.findViewById(R.id.tv_msg);
        TextView tv_time=v.findViewById(R.id.tv_time);

        List<Msg> msgs=msgHelper.query("SELECT * FROM msg WHERE srcUid='"+user.getId()+"' and status="+Msg.STATUS_UNREAD+" ORDER BY time");
        if(msgs==null || msgs.isEmpty()){
            Msg msg= (Msg) msgHelper.querySingle("SELECT * FROM msg WHERE srcUid='"+user.getId()+"' or dstUid='"+user.getId()+"' ORDER BY time DESC");
            if(msg==null){
                tv_msg.setText("无消息");
                tv_time.setVisibility(View.GONE);
            }else{
                tv_msg.setText(msg.getContent());
                tv_time.setText(TimeUtils.toStandardTime(msg.getTime()));
            }
        }else{
            tv_msg.setText("有 "+msgs.size()+" 条未读消息");
            tv_msg.setTextColor(Color.RED);
            tv_time.setText(TimeUtils.toStandardTime(msgs.get(0).getTime()));
        }

        tv_name.setText(user.getName());

        return v;
    }
}
