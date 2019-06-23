package com.nasa.bt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
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

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.SecretChat;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.utils.CommonDbHelper;
import com.nasa.bt.utils.LocalDbUtils;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecretChatListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ListView lv_secret_chats;
    private List<SecretChat> secretChats;
    private CommonDbHelper secretChatHelper;
    private ProgressBar pb;

    private Handler changeHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            pb.setVisibility(View.GONE);
            refresh();
        }
    };

    private Handler reportHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            ActionReport actionReport= JSON.parseObject(params.get("action_report"),ActionReport.class);

            if(!actionReport.getActionIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_CREATE_SECRET_CHAT))
                return;
            pb.setVisibility(View.GONE);
            if(actionReport.getActionStatus().equals(ActionReport.STATUS_SUCCESS))
                Toast.makeText(SecretChatListActivity.this,"创建成功",Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(SecretChatListActivity.this,"创建失败",Toast.LENGTH_SHORT).show();
            refresh();
        }
    };

    private MessageIntent intentSecretChat=new MessageIntent("SECRET_CHAT_LIST_SECRET_CHAT", Datagram.IDENTIFIER_RETURN_SECRET_CHAT,changeHandler,0,1);
    private MessageIntent intentReport=new MessageIntent("SECRET_CHAT_LIST_REPORT", Datagram.IDENTIFIER_REPORT,reportHandler,0,1);
    private MessageIntent intentMessage=new MessageIntent("SECRET_CHAT_LIST_MESSAGE", Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,changeHandler,0,1);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret_chat_list);

        MessageLoop.addIntent(intentSecretChat);
        MessageLoop.addIntent(intentReport);
        MessageLoop.addIntent(intentMessage);

        secretChatHelper= LocalDbUtils.getSecretChatHelper(this);
        lv_secret_chats=findViewById(R.id.lv_secret_chats);
        pb=findViewById(R.id.pb);

        lv_secret_chats.setOnItemClickListener(this);
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_secret_chat_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        refresh();
    }

    private void refresh(){
        secretChats=secretChatHelper.query();
        lv_secret_chats.setAdapter(new SecretChatsAdapter(secretChats,this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.m_add_secret_chat){
            AlertDialog.Builder builder=new AlertDialog.Builder(this);

            View v=View.inflate(this,R.layout.view_create_secret_chat,null);
            final TextInputEditText et_name,et_key;
            et_name=v.findViewById(R.id.et_name);
            et_key=v.findViewById(R.id.et_key);

            builder.setTitle("创建私密聊天");
            builder.setCancelable(false);
            builder.setNegativeButton("取消",null);
            builder.setPositiveButton("创建", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String name=et_name.getText().toString();
                    String key=et_key.getText().toString();

                    if(TextUtils.isEmpty(name) || TextUtils.isEmpty(key)){
                        Toast.makeText(SecretChatListActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String keyHash= SHA256Utils.getSHA256InHex(key);

                    Map<String,String> params=new HashMap<>();
                    params.put("dst_name",name);
                    params.put("key_hash",keyHash);
                    Datagram datagram=new Datagram(Datagram.IDENTIFIER_CREATE_SECRET_CHAT,params,null);
                    LoopResource.sendDatagram(datagram);
                    pb.setVisibility(View.VISIBLE);
                }
            });
            builder.setView(v);
            builder.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final SecretChat secretChat=secretChats.get(i);
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("请输入此加密聊天的密码");

        final EditText et=new EditText(this);
        et.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(et);
        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String key=et.getText().toString();
                String keyHash=SHA256Utils.getSHA256InHex(key);
                if(!keyHash.equals(secretChat.getKeyHash())){
                    Toast.makeText(SecretChatListActivity.this,"密码不正确",Toast.LENGTH_SHORT).show();
                    return;
                }else{
                    Intent intent=new Intent(SecretChatListActivity.this,SecretChatActivity.class);
                    intent.putExtra("secret_chat",secretChat);
                    intent.putExtra("key",key);
                    startActivity(intent);
                }
            }
        });
        builder.show();
    }
}

class SecretChatsAdapter extends BaseAdapter{

    private List<SecretChat> secretChats;
    private Context context;
    private CommonDbHelper msgHelper;
    private CommonDbHelper userHelper;

    public SecretChatsAdapter(List<SecretChat> secretChats, Context context) {
        this.secretChats = secretChats;
        this.context = context;
        msgHelper=LocalDbUtils.getMsgHelper(context);
        userHelper=LocalDbUtils.getUserInfoHelper(context);
    }

    @Override
    public int getCount() {
        if(secretChats==null)
            return 0;
        return secretChats.size();
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
        SecretChat secretChat=secretChats.get(i);

        View v=View.inflate(context,R.layout.view_main_user,null);

        TextView tv_name=v.findViewById(R.id.tv_name);
        TextView tv_msg=v.findViewById(R.id.tv_msg);
        TextView tv_time=v.findViewById(R.id.tv_time);

        String uid= LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_UID);
        String dstUid=(uid.equals(secretChat.getSrcUid()))?secretChat.getDstUid():secretChat.getSrcUid();

        UserInfo userInfo= (UserInfo) userHelper.querySingle("SELECT * FROM userinfo WHERE id='"+dstUid+"'");
        if(userInfo!=null)
            tv_name.setText("与 "+userInfo.getName()+" 的私密聊天");
        else
            tv_name.setText("未知用户");

        List<Msg> msgs=msgHelper.query("SELECT * FROM msg WHERE srcUid='"+secretChat.getSessionId()+"' and status="+Msg.STATUS_UNREAD+" ORDER BY time");
        if(msgs==null || msgs.isEmpty()){
            tv_msg.setText("暂无未读消息");
            tv_time.setVisibility(View.GONE);
        }else{
            tv_msg.setText("有 "+msgs.size()+" 条未读消息");
            tv_msg.setTextColor(Color.RED);
            tv_time.setText(TimeUtils.toStandardTime(msgs.get(0).getTime()));
        }

        return v;
    }
}
