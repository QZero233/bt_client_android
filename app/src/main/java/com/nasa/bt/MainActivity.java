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
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.Session;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.utils.CommonDbHelper;
import com.nasa.bt.utils.LocalDbUtils;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {


    private Handler changeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            refresh();
            if (sl_main.isRefreshing()) {
                sl_main.setRefreshing(false);
                Toast.makeText(MainActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private ListView lv_sessions;
    private SwipeRefreshLayout sl_main;

    private List<Session> sessions;
    private CommonDbHelper sessionHelper;

    MessageIntent intentMessage = new MessageIntent("MAIN_MESSAGE", Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL, changeHandler, 0, 1);
    MessageIntent intentMessageIndex = new MessageIntent("MAIN_MESSAGE_INDEX", Datagram.IDENTIFIER_RETURN_MESSAGE_INDEX, changeHandler, 0, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MessageLoop.addIntent(intentMessage);
        MessageLoop.addIntent(intentMessageIndex);

        KeyUtils.initContext(this);

        String name, code;
        name = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_NAME);
        code = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_CODE_HASH);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(code)) {
            startActivity(new Intent(this, AuthInfoActivity.class));
            Toast.makeText(this, "请设置基本信息", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionHelper = LocalDbUtils.getSessionHelper(this);

        startService(new Intent(this, MessageLoopService.class));

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
        sessions = sessionHelper.queryOrder("lastTime DESC");
        lv_sessions.setAdapter(new MainUserAdapter(sessions,this));
    }

    private void startChat(final int index) {
        final Session session = sessions.get(index);
        if (session.getSessionType() == Session.TYPE_NORMAL) {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("session", session);
            startActivity(intent);
            return;
        } else if (session.getSessionType() == Session.TYPE_SECRET_CHAT) {
            Map<String, String> sessionParams = session.getParamsInMap();
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
                        Toast.makeText(MainActivity.this, "密码不正确", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                        intent.putExtra("session", session);
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
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
                    LocalSettingsUtils.save(MainActivity.this, LocalSettingsUtils.FIELD_SERVER_IP, newIp);
                    Toast.makeText(MainActivity.this, "修改成功", Toast.LENGTH_SHORT).show();

                    Datagram datagram = new Datagram(LoopResource.INBOX_IDENTIFIER_RECONNECT, null);
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
            Datagram datagramDisconnect = new Datagram(LoopResource.INBOX_IDENTIFIER_DISCONNECTED, null);
            MessageLoop.processDatagram(datagramDisconnect);
            Toast.makeText(this, "退出成功", Toast.LENGTH_SHORT).show();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        Datagram datagram = new Datagram(Datagram.IDENTIFIER_GET_MESSAGE_INDEX, null);
        LoopResource.sendDatagram(datagram);

        //datagram.setIdentifier(Datagram.IDENTIFIER_GET_SESSIONS_INDEX);
        //LoopResource.sendDatagram(datagram);
    }
}

class MainUserAdapter extends BaseAdapter {

    private List<Session> sessions;
    private Context context;
    private CommonDbHelper msgHelper, userHelper;

    public MainUserAdapter(List<Session> sessions, Context context) {
        this.sessions = sessions;
        this.context = context;
        msgHelper = LocalDbUtils.getMsgHelper(context);
        userHelper = LocalDbUtils.getUserInfoHelper(context);
    }

    @Override
    public int getCount() {
        if (sessions == null || sessions.isEmpty())
            return 0;
        return sessions.size();
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
        Session session = sessions.get(i);

        View v = View.inflate(context, R.layout.view_main_user, null);

        TextView tv_name = v.findViewById(R.id.tv_name);
        TextView tv_msg = v.findViewById(R.id.tv_msg);
        TextView tv_time = v.findViewById(R.id.tv_time);

        String dstUid = session.getIdOfOther(LocalSettingsUtils.read(context, LocalSettingsUtils.FIELD_UID));

        List<Msg> msgs = msgHelper.query("SELECT * FROM msg WHERE srcUid='" + dstUid + "' and sessionId='"+session.getSessionId()+"' and status=" + Msg.STATUS_UNREAD + " ORDER BY time");
        if (msgs == null || msgs.isEmpty()) {
            if (TextUtils.isEmpty(session.getLastMessage())) {
                tv_msg.setText("无消息");
                tv_time.setVisibility(View.GONE);
            } else {
                tv_msg.setText(session.getLastMessage());
                tv_time.setText(TimeUtils.toStandardTime(session.getLastTime()));
            }

            if(session.getSessionType()==Session.TYPE_SECRET_CHAT)
                tv_msg.setText("加密信息，需密码解密查看");
        } else {
            tv_msg.setText("有 " + msgs.size() + " 条未读消息");
            tv_msg.setTextColor(Color.RED);
            tv_time.setText(TimeUtils.toStandardTime(msgs.get(0).getTime()));
        }

        UserInfo userInfo = (UserInfo) userHelper.querySingle("SELECT * FROM userinfo WHERE id='" + dstUid + "'");
        if (userInfo != null)
            tv_name.setText(userInfo.getName());
        else {
            tv_name.setText("未知用户");
        }

        if(session.getSessionType()==Session.TYPE_SECRET_CHAT){
            tv_name.setText(tv_name.getText()+"(加密聊天)");
            tv_name.setTextColor(Color.RED);
        }

        return v;
    }
}
