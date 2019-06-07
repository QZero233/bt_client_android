package com.nasa.bt;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.LoginInfo;
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.utils.CommonDbHelper;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.TimeUtils;
import com.nasa.bt.utils.UUIDUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();

            if(params.get("action_identifier").equalsIgnoreCase(Datagram.IDENTIFIER_UPDATE_USER_INFO)){
                pullUserInfo();
                MessageLoop.removeIntent(Datagram.IDENTIFIER_REPORT,intent.getId(),1);
                return;
            }

            if(!params.get("action_identifier").equals(Datagram.IDENTIFIER_SIGN_IN))
                return;


            if(params.get("action_status").equals("0")){
                LocalSettingsUtils.save(MainActivity.this,LocalSettingsUtils.FIELD_NAME,"");
                LocalSettingsUtils.save(MainActivity.this,LocalSettingsUtils.FIELD_CODE_HASH,"");
                LocalSettingsUtils.save(MainActivity.this,LocalSettingsUtils.FIELD_SID,"");
                Toast.makeText(MainActivity.this,"身份验证失败，请重新输入信息",Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String sid=params.get("more");
            Log.e("NASA",sid+" NEW MSG");

            LocalSettingsUtils.save(MainActivity.this,LocalSettingsUtils.FIELD_SID,sid);

            Toast.makeText(MainActivity.this,"身份验证成功",Toast.LENGTH_SHORT).show();

            updateUserInfo();

        }
    };

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
                    //TODO 取消进度条
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

    private void updateUserInfo(){
        //TODO 上传自己的公钥
        String key="wdnmd";//TODO 得到公钥
        Map<String,byte[]> params=new HashMap<>();
        params.put("key",key.getBytes());
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_UPDATE_USER_INFO,params);
        LoopResource.sendDatagram(datagram);
    }

    private void pullUserInfo(){
        Datagram datagramPullUser=new Datagram(Datagram.IDENTIFIER_GET_USERS_INDEX,null);
        LoopResource.sendDatagram(datagramPullUser);
    }

    MessageIntent intent=new MessageIntent("MAIN_REPORT", Datagram.IDENTIFIER_REPORT,handler,0,1);
    MessageIntent intentUserIndex=new MessageIntent("MAIN_USER_INDEX", Datagram.IDENTIFIER_RETURN_USERS_INDEX,userHandler,0,1);
    MessageIntent intentUserInfo=new MessageIntent("MAIN_USER_INFO", Datagram.IDENTIFIER_RETURN_USER_INFO,userHandler,0,1);
    MessageIntent intentMessage=new MessageIntent("MAIN_MESSAGE", Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,msgHandler,0,1);

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageLoop.removeIntent(Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,intentMessage.getId(),1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MessageLoop.addIntent(intent);
        MessageLoop.addIntent(intentUserIndex);
        MessageLoop.addIntent(intentUserInfo);
        MessageLoop.addIntent(intentMessage);

        LoginInfo info=new LoginInfo();
        if(TextUtils.isEmpty(LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_SID))){
            String name,code;
            name=LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_NAME);
            code=LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_CODE_HASH);
            if(TextUtils.isEmpty(name) || TextUtils.isEmpty(code)){
                startActivity(new Intent(this,AuthInfoActivity.class));
                Toast.makeText(this,"请设置基本信息",Toast.LENGTH_SHORT).show();
                return;
            }

            info.name=name;
            info.codeHash=code;
            LoopResource.loginInfo=info;
        }else{
            info.sid=LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_SID);
            LoopResource.loginInfo=info;
        }

        stopService(new Intent(this, MessageLoopService.class));
        startService(new Intent(this, MessageLoopService.class));
        LoopResource.cleanUnsent();

        userHelper=new CommonDbHelper(this,UserInfo.class,"");
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
}

class MainUserAdapter extends BaseAdapter{

    private List<UserInfo> users;
    private Context context;
    private CommonDbHelper msgHelper;

    public MainUserAdapter(List<UserInfo> users, Context context) {
        this.users = users;
        this.context = context;
        msgHelper=new CommonDbHelper(context, Msg.class,"");
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
