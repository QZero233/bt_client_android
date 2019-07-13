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

    MessageIntent intentMessage = new MessageIntent("MAIN_MESSAGE", Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL, changeHandler, 0, 1);
    MessageIntent intentSessionIndex = new MessageIntent("MAIN_SESSION_INDEX", Datagram.IDENTIFIER_RETURN_SESSIONS_INDEX, changeHandler, 0, 1);
    MessageIntent intentUserInfo = new MessageIntent("MAIN_USER_INFO", Datagram.IDENTIFIER_RETURN_USER_INFO, changeHandler, 0, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        MessageLoop.addIntent(intentMessage);
        MessageLoop.addIntent(intentSessionIndex);
        MessageLoop.addIntent(intentUserInfo);

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

    private void refresh() {
        sessionEntities=sessionDao.getAllSession();
        lv_sessions.setAdapter(new SessionListAdapter(sessionEntities,this));
    }

    private void startChat(final int index) {
        final SessionEntity sessionEntity = sessionEntities.get(index);
        if (sessionEntity.getSessionType() == SessionEntity.TYPE_NORMAL) {
            Intent intent = new Intent(SessionListActivity.this, ChatActivity.class);
            intent.putExtra("sessionEntity", sessionEntity);
            startActivity(intent);
            return;
        } else if (sessionEntity.getSessionType() == SessionEntity.TYPE_SECRET_CHAT) {
            Map<String, String> sessionParams = sessionEntity.getParamsInMap();
            final String sessionKeyHash = sessionParams.get("key");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("请输入此加密聊天的密码");

            final EditText et = new EditText(this);
            et.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(et);
            builder.setNegativeButton("取消", null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String key = et.getText().toString();
                    String keyHash = SHA256Utils.getSHA256InHex(key);
                    if (!keyHash.equals(sessionKeyHash)) {
                        Toast.makeText(SessionListActivity.this, "密码不正确", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        Intent intent = new Intent(SessionListActivity.this, ChatActivity.class);
                        intent.putExtra("sessionEntity", sessionEntity);
                        intent.putExtra("key", key);
                        startActivity(intent);
                    }
                }
            });
            builder.show();
            return;
        }
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
        Datagram datagram = new Datagram(Datagram.IDENTIFIER_GET_MESSAGE_INDEX, null);
        MessageLoopResource.sendDatagram(datagram);

        Datagram datagram2=new Datagram(Datagram.IDENTIFIER_GET_SESSIONS_INDEX, null);
        MessageLoopResource.sendDatagram(datagram2);

        refresh();
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
                tv_msg.setText(sessionEntity.getLastMessage());
                tv_time.setText(TimeUtils.toStandardTime(sessionEntity.getLastTime()));
            }

            if(sessionEntity.getSessionType()== SessionEntity.TYPE_SECRET_CHAT)
                tv_msg.setText("加密信息，需密码解密查看");
        } else {
            tv_msg.setText("有 " + messageEntities.size() + " 条未读消息");
            tv_msg.setTextColor(Color.RED);
            tv_time.setText(TimeUtils.toStandardTime(messageEntities.get(0).getTime()));
        }

        UserInfoEntity userInfoEntity =userInfoDao.getUserInfoById(dstUid);
        if (userInfoEntity != null)
            tv_name.setText(userInfoEntity.getName());
        else {
            tv_name.setText("未知用户");
        }

        if(sessionEntity.getSessionType()== SessionEntity.TYPE_SECRET_CHAT){
            tv_name.setText(tv_name.getText()+"(加密聊天)");
            tv_name.setTextColor(Color.RED);
        }

        return v;
    }
}
