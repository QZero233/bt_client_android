package com.nasa.bt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ProgressBar;
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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private int userCount=0;
    private int currentCount=0;
    Handler userHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            if(datagram.getIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_RETURN_USERS_INDEX)){
                userCount=datagram.getParamsAsString().get("index").length()/36;
            }else{
                currentCount++;
                if(currentCount>=userCount){
                    Toast.makeText(MainActivity.this,"用户信息同步完成",Toast.LENGTH_SHORT).show();
                    doMain();

                    MessageLoop.removeIntent(Datagram.IDENTIFIER_RETURN_USERS_INDEX,intentUserIndex.getId(),1);
                    MessageLoop.removeIntent(Datagram.IDENTIFIER_RETURN_USER_INFO,intentUserInfo.getId(),1);
                }
            }

        }
    };

    Handler msgHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            reloadUserInfo();
        }
    };

    private ProgressBar pb_main;
    private ListView lv_users;
    private List<UserInfo> users;
    private CommonDbHelper userHelper;

    MessageIntent intentUserIndex=new MessageIntent("MAIN_USER_INDEX", Datagram.IDENTIFIER_RETURN_USERS_INDEX,userHandler,0,1);
    MessageIntent intentUserInfo=new MessageIntent("MAIN_USER_INFO", Datagram.IDENTIFIER_RETURN_USER_INFO,userHandler,0,1);
    MessageIntent intentMessage=new MessageIntent("MAIN_MESSAGE", Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,msgHandler,0,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MessageLoop.addIntent(intentUserIndex);
        MessageLoop.addIntent(intentUserInfo);
        MessageLoop.addIntent(intentMessage);

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

        stopService(new Intent(this, MessageLoopService.class));
        startService(new Intent(this, MessageLoopService.class));
        LoopResource.cleanUnsent();

        userHelper= LocalDbUtils.getUserInfoHelper(this);
        pb_main=findViewById(R.id.pb_main);
        lv_users=findViewById(R.id.lv_users);
    }

    private void doMain(){
        pb_main.setVisibility(View.GONE);
        lv_users.setClickable(true);

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
        users=userHelper.query("SELECT * FROM userinfo WHERE id!='"+LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_UID)+"'");
        lv_users.setAdapter(new MainUserAdapter(users,this));
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
        }
        return super.onOptionsItemSelected(item);
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
            tv_msg.setText("无消息");
            tv_time.setVisibility(View.GONE);
        }else{
            tv_msg.setText("有 "+msgs.size()+" 条未读消息");
            tv_msg.setTextColor(Color.RED);
            tv_time.setText(TimeUtils.toStandardTime(msgs.get(0).getTime()));
        }

        tv_name.setText(user.getName());

        return v;
    }
}
