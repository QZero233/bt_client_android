package com.nasa.bt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.loop.MessageLoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.session.JoinSessionCallback;
import com.nasa.bt.session.SessionProcessor;
import com.nasa.bt.session.SessionProcessorFactory;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.TimeUtils;

import java.util.List;
import java.util.Map;

/**
 * 心中有党，成绩理想
 */
public class SessionListActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {


    private Handler changeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            refresh();
        }
    };

    private Handler refreshHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            ActionReport actionReport= JSON.parseObject(datagram.getParamsAsString().get("action_report"),ActionReport.class);
            if(!actionReport.getActionIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_REFRESH))
                return;

            refresh();
            if (sl_main.isRefreshing()) {
                sl_main.setRefreshing(false);
                Toast.makeText(SessionListActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private ListView lv_sessions;
    private SwipeRefreshLayout sl_main;

    private List<SessionEntity> sessionEntities;

    private SessionDao sessionDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        MessageLoop.addIntent(new MessageIntent("SESSION_LIST_MESSAGE", Datagram.IDENTIFIER_MESSAGE_DETAIL, changeHandler, 0, 1));
        MessageLoop.addIntent(new MessageIntent("SESSION_LIST_SESSION", Datagram.IDENTIFIER_SESSION_DETAIL, changeHandler, 0, 1));
        MessageLoop.addIntent(new MessageIntent("SESSION_LIST_USER_INFO", Datagram.IDENTIFIER_USER_INFO, changeHandler, 0, 1));
        MessageLoop.addIntent(new MessageIntent("SESSION_LIST_UPDATE", Datagram.IDENTIFIER_UPDATE_DETAIL, changeHandler, 0, 1));
        MessageLoop.addIntent(new MessageIntent("SESSION_LIST_REFRESH_REPORT",Datagram.IDENTIFIER_REPORT,refreshHandler,0,1));

        sessionDao=new SessionDao(this);

        lv_sessions = findViewById(R.id.lv_users);
        sl_main = findViewById(R.id.sl_main);

        sl_main.setOnRefreshListener(this);
        lv_sessions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startChat(i);
            }
        });
        refresh();
        setTitle("当前" + LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_NAME));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        MessageLoop.removeIntent(Datagram.IDENTIFIER_MESSAGE_DETAIL,"SESSION_LIST_MESSAGE",1);
        MessageLoop.removeIntent(Datagram.IDENTIFIER_SESSION_DETAIL,"SESSION_LIST_SESSION",1);
        MessageLoop.removeIntent(Datagram.IDENTIFIER_USER_INFO,"SESSION_LIST_USER_INFO",1);
        MessageLoop.removeIntent(Datagram.IDENTIFIER_REPORT,"SESSION_LIST_REFRESH_REPORT",1);
        MessageLoop.removeIntent(Datagram.IDENTIFIER_UPDATE_DETAIL,"SESSION_LIST_UPDATE",1);
    }

    private void refresh() {
        sessionEntities=sessionDao.getAllSession();
        lv_sessions.setAdapter(new SessionListAdapter(sessionEntities,this));
    }

    private void startChat(final int index) {
        final SessionEntity sessionEntity = sessionEntities.get(index);
        SessionProcessor processor= SessionProcessorFactory.getProcessor(sessionEntity.getSessionType());
        if(processor==null)
            return;

        JoinSessionCallback callback=new JoinSessionCallback() {
            @Override
            public void start(Intent intentWithParams) {
                if(intentWithParams ==null)
                    intentWithParams=new Intent();
                intentWithParams.putExtra("sessionEntity",sessionEntity);
                intentWithParams.setClass(SessionListActivity.this,ChatActivity.class);
                startActivity(intentWithParams);
            }
        };

        processor.joinSession(sessionEntity,callback,this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_session_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.m_settings) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            final EditText et_ip = new EditText(this);
            String ip = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_SERVER_IP);
            if (TextUtils.isEmpty(ip))
                ip = MessageLoopService.SERVER_IP_DEFAULT;
            et_ip.setText(ip);

            builder.setView(et_ip);
            builder.setMessage("请输入服务器IP");
            builder.setNegativeButton("取消", null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String newIp = et_ip.getText().toString();
                    if(TextUtils.isEmpty(newIp))
                        newIp=MessageLoopService.SERVER_IP_DEFAULT;
                    LocalSettingsUtils.save(SessionListActivity.this, LocalSettingsUtils.FIELD_SERVER_IP, newIp);
                    Toast.makeText(SessionListActivity.this, "修改成功", Toast.LENGTH_SHORT).show();

                    Datagram datagram = new Datagram(MessageLoopResource.INBOX_IDENTIFIER_RECONNECT, null);
                    MessageLoop.processDatagram(datagram);

                    finish();
                }
            });
            builder.show();
        } else if (item.getItemId() == R.id.m_reset_key) {
            try {
                KeyUtils utils = KeyUtils.getInstance();
                utils.genKeySet();
                utils.saveKeySet();

                MessageLoopResource.sendDatagram(new Datagram(MessageLoopResource.INBOX_IDENTIFIER_RECONNECT,null));

                Toast.makeText(this, "重置成功", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "重置失败", Toast.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == R.id.m_contact) {
            startActivity(new Intent(this, ContactActivity.class));
        } else if (item.getItemId() == R.id.m_quit) {
            LocalSettingsUtils.save(this, LocalSettingsUtils.FIELD_NAME, "");
            LocalSettingsUtils.save(this, LocalSettingsUtils.FIELD_CODE_HASH, "");
            LocalSettingsUtils.save(this, LocalSettingsUtils.FIELD_CODE_LAST, "");
            Datagram datagramDisconnect = new Datagram(MessageLoopResource.INBOX_IDENTIFIER_DISCONNECTED, null);
            MessageLoop.processDatagram(datagramDisconnect);
            Toast.makeText(this, "退出成功", Toast.LENGTH_SHORT).show();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_REFRESH,null);
        MessageLoopResource.sendDatagram(datagram);
    }
}

class SessionListAdapter extends BaseAdapter {

    private List<SessionEntity> sessionEntities;
    private Context context;
    private MessageDao messageDao;
    private UserInfoDao userInfoDao;

    public SessionListAdapter(List<SessionEntity> sessionEntities, Context context) {
        this.sessionEntities = sessionEntities;
        this.context = context;

        messageDao=new MessageDao(context);
        userInfoDao=new UserInfoDao(context);
    }

    @Override
    public int getCount() {
        if (sessionEntities == null || sessionEntities.isEmpty())
            return 0;
        return sessionEntities.size();
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
        SessionEntity sessionEntity = sessionEntities.get(i);
        SessionProcessor processor=SessionProcessorFactory.getProcessor(sessionEntity.getSessionType());
        if(processor==null)
            return null;

        View v = View.inflate(context, R.layout.view_main_user, null);

        TextView tv_name = v.findViewById(R.id.tv_name);
        TextView tv_msg = v.findViewById(R.id.tv_msg);
        TextView tv_time = v.findViewById(R.id.tv_time);

        String dstUid = sessionEntity.getIdOfOther(LocalSettingsUtils.read(context, LocalSettingsUtils.FIELD_UID));

        List<MessageEntity> messageEntities =messageDao.getUnreadMessageBySessionId(sessionEntity.getSessionId());
        if (messageEntities == null || messageEntities.isEmpty()) {
            if (TextUtils.isEmpty(sessionEntity.getLastMessage())) {
                tv_msg.setText("无消息");
                tv_time.setVisibility(View.GONE);
            } else {
                tv_msg.setText(processor.getMessageMain(sessionEntity));
                tv_time.setText(TimeUtils.toStandardTime(sessionEntity.getLastTime()));
            }
        } else {
            tv_msg.setText("有 " + messageEntities.size() + " 条未读消息");
            tv_msg.setTextColor(Color.RED);
            tv_time.setText(TimeUtils.toStandardTime(messageEntities.get(0).getTime()));
        }

        UserInfoEntity userInfoEntity =userInfoDao.getUserInfoById(dstUid);
        if (userInfoEntity != null)
            tv_name.setText(userInfoEntity.getName()+processor.getMainNameEndWith());
        else {
            tv_name.setText("未知用户");
        }
        if(processor.getSessionTextColor()!=-1)
            tv_name.setTextColor(processor.getSessionTextColor());

        if(sessionEntity.isDisabled()){
            tv_name.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        }


        return v;
    }
}
