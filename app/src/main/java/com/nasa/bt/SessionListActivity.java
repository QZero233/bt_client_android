package com.nasa.bt;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.ContactEntity;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.loop.ActionReportListener;
import com.nasa.bt.loop.DatagramListener;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.session.JoinSessionCallback;
import com.nasa.bt.session.SessionProcessor;
import com.nasa.bt.session.SessionProcessorFactory;
import com.nasa.bt.session.SessionProperties;
import com.nasa.bt.upgrade.UpgradeStatus;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.NotificationUtils;
import com.nasa.bt.utils.TimeUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 心中有党，成绩理想
 */
public class SessionListActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemLongClickListener,NavigationView.OnNavigationItemSelectedListener {


    private DatagramListener changedListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            reloadSessionList();
        }
    };

    private ActionReportListener syncReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            setDrawerHeadText();
        }
    };

    private ActionReportListener refreshReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            new NotificationUtils(SessionListActivity.this).cancelNotification();
            reloadSessionList();
            if (sl_main.isRefreshing()) {
                sl_main.setRefreshing(false);
                Toast.makeText(SessionListActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
            }
            setTitle("BugTelegram内测版");
        }
    };

    private ActionReportListener authReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            if(!actionReport.getActionStatusInBoolean()){
                //身份验证未通过
                startActivity(new Intent(SessionListActivity.this,AuthInfoActivity.class));
                finish();
            }
        }
    };

    private DatagramListener connectionStatusListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            int status=Integer.parseInt(datagram.getParamsAsString().get("status"));
            switch (status){
                case MessageLoopService.STATUS_CONNECTED:
                    sync();
                    refresh();
                    break;
                case MessageLoopService.STATUS_CONNECTING:
                    setTitle("正在连接服务器");
                    break;
                case MessageLoopService.STATUS_DISCONNECTED:
                    setTitle("已断线");
                    break;
            }
        }
    };

    private DatagramListener upgradeDetailListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            String json=datagram.getParamsAsString().get("upgrade_status");
            checkUpgrade(JSON.parseObject(json,UpgradeStatus.class));
        }
    };

    private ListView lv_sessions;
    private SwipeRefreshLayout sl_main;
    private TextView tv_name;
    private DrawerLayout dl;

    private List<SessionEntity> sessionEntities;

    private SessionDao sessionDao;
    private UserInfoDao userInfoDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        /**
         * 打开主界面之后依次进行
         * 检查更新信息
         * 检查是否输入身份验证信息，如果没有就跳转并finish
         * 连接服务器（无论是否已连接都重连，主窗口监听连接情况，如果身份验证失败就跳转&finish）
         * 同步（告诉服务器本地已有的会话ID等，服务器返回客户端没有的）
         * 刷新（服务器返回新消息或新更新）
         */

        if(!checkIfLocalAuthInfoExists()){
            startActivity(new Intent(this, AuthInfoActivity.class));
            Toast.makeText(this, "请设置基本信息", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        LocalDatabaseHelper.reset(this);

        startService(new Intent(this, MessageLoopService.class));

        MessageLoopUtils.registerActionReportListenerNormal("SESSION_LIST_REFRESH_REPORT",Datagram.IDENTIFIER_REFRESH,refreshReportListener);
        MessageLoopUtils.registerActionReportListenerNormal("SESSION_LIST_AUTH_REPORT",Datagram.IDENTIFIER_SIGN_IN,authReportListener);
        MessageLoopUtils.registerActionReportListenerNormal("SESSION_LIST_SYNC_REPORT",Datagram.IDENTIFIER_SYNC,syncReportListener);

        MessageLoopUtils.registerListenerNormal("SESSION_LIST_MESSAGE",Datagram.IDENTIFIER_MESSAGE_DETAIL,changedListener);
        MessageLoopUtils.registerListenerNormal("SESSION_LIST_SESSION",Datagram.IDENTIFIER_SESSION_DETAIL,changedListener);
        MessageLoopUtils.registerListenerNormal("SESSION_LIST_USER_INFO",Datagram.IDENTIFIER_USER_INFO,changedListener);
        MessageLoopUtils.registerListenerNormal("SESSION_LIST_UPDATE",Datagram.IDENTIFIER_UPDATE_DETAIL,changedListener);
        MessageLoopUtils.registerListenerNormal("SESSION_LIST_UPGRADE",Datagram.IDENTIFIER_UPGRADE_DETAIL,upgradeDetailListener);

        MessageLoopUtils.registerListenerNormal("SESSION_LIST_CONNECTION_STATUS",SendDatagramUtils.INBOX_IDENTIFIER_CONNECTION_STATUS,connectionStatusListener);

        sessionDao=new SessionDao(this);
        userInfoDao=new UserInfoDao(this);

        initDrawer();

        lv_sessions = findViewById(R.id.lv_users);
        sl_main = findViewById(R.id.sl_main);


        sl_main.setOnRefreshListener(this);
        lv_sessions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startChat(i);
            }
        });
        lv_sessions.setOnItemLongClickListener(this);

        reloadSessionList();
        setTitle("当前" + LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_NAME));


    }

    private void setDrawerHeadText(){
        tv_name.setText("正在同步......");

        String uid=LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_UID);
        if(TextUtils.isDigitsOnly(uid))
            return;

        UserInfoEntity userInfoEntity=userInfoDao.getUserInfoById(uid);
        if(userInfoEntity==null)
            return;

        tv_name.setText("欢迎用户 "+userInfoEntity.getName());
    }

    private void initDrawer(){

        dl=findViewById(R.id.dl);
        NavigationView nv=findViewById(R.id.nv);
        Toolbar tb=findViewById(R.id.tb);
        setSupportActionBar(tb);

        tb.setTitleTextColor(Color.WHITE);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, dl, tb,R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        dl.addDrawerListener(toggle);
        toggle.syncState();

        nv.setNavigationItemSelectedListener(this);

        tv_name=nv.getHeaderView(0).findViewById(R.id.tv_name);
        setDrawerHeadText();

        setDrawerLeftEdgeSize(this,dl,0.3F);
    }

    private void setDrawerLeftEdgeSize (Activity activity, DrawerLayout drawerLayout, float displayWidthPercentage) {
        if (activity == null || drawerLayout == null) return;
        try {
            // 找到 ViewDragHelper 并设置 Accessible 为true
            Field leftDraggerField =
                    drawerLayout.getClass().getDeclaredField("mLeftDragger");//Right
            leftDraggerField.setAccessible(true);
            ViewDragHelper leftDragger = (ViewDragHelper) leftDraggerField.get(drawerLayout);

            // 找到 edgeSizeField 并设置 Accessible 为true
            Field edgeSizeField = leftDragger.getClass().getDeclaredField("mEdgeSize");
            edgeSizeField.setAccessible(true);
            int edgeSize = edgeSizeField.getInt(leftDragger);

            // 设置新的边缘大小
            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            edgeSizeField.setInt(leftDragger, Math.max(edgeSize, (int) (displaySize.x *
                    displayWidthPercentage)));
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.m_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                finish();
                break;
            case R.id.m_contact:
                startActivity(new Intent(this, ContactActivity.class));
                break;
            case R.id.m_about:
                Toast.makeText(this,"某勤奋的作者：此功能未完成（手动滑稽）",Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    private boolean  checkUpgrade(final UpgradeStatus upgradeStatus){
        if(upgradeStatus!=null){
            AlertDialog.Builder builder=new AlertDialog.Builder(SessionListActivity.this);
            builder.setTitle("发现新版本 "+upgradeStatus.getNewestName());
            builder.setMessage("更新日志：\n"+upgradeStatus.getUpgradeLog());
            builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            builder.setPositiveButton("更新", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent=new Intent(Intent.ACTION_VIEW);
                    Uri uri=Uri.parse(upgradeStatus.getDownloadUrl());
                    intent.setData(uri);
                    startActivity(intent);

                    finish();
                }
            });
            builder.setCancelable(false);
            builder.show();
            return true;
        }
        return false;
    }

    private boolean checkIfLocalAuthInfoExists(){
        String name = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_NAME);
        String code = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_CODE_HASH);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(code)) {
            return false;
        }
        return true;
    }

    private void refresh(){
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_REFRESH,null);
        SendDatagramUtils.sendDatagram(datagram);
        setTitle("刷新中......");
    }

    private void sync(){
        String sessionIds="";
        for(SessionEntity sessionEntity:sessionEntities){
            sessionIds+=sessionEntity.getSessionId();
        }
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_SYNC,new ParamBuilder().putParam("session_id",sessionIds).build());
        SendDatagramUtils.sendDatagram(datagram);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        MessageLoopUtils.unregisterListener("SESSION_LIST_REFRESH_REPORT");
        MessageLoopUtils.unregisterListener("SESSION_LIST_AUTH_REPORT");
        MessageLoopUtils.unregisterListener("SESSION_LIST_SYNC_REPORT");
        MessageLoopUtils.unregisterListener("SESSION_LIST_MESSAGE");
        MessageLoopUtils.unregisterListener("SESSION_LIST_SESSION");
        MessageLoopUtils.unregisterListener("SESSION_LIST_USER_INFO");
        MessageLoopUtils.unregisterListener("SESSION_LIST_UPDATE");
        MessageLoopUtils.unregisterListener("SESSION_LIST_UPGRADE");
        MessageLoopUtils.unregisterListener("SESSION_LIST_CONNECTION_STATUS");
    }

    private void reloadSessionList() {
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
        reloadSessionList();
    }

    @Override
    public void onRefresh() {
        refresh();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(this, SessionDetailActivity.class);
        intent.putExtra("sessionEntity", sessionEntities.get(i));
        startActivity(intent);
        return true;
    }

    @Override
    public void onBackPressed() {
        if(dl.isDrawerOpen(GravityCompat.START)){
            dl.closeDrawer(GravityCompat.START);
        }else
            super.onBackPressed();
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
        SessionProperties sessionProperties=processor.getSessionProperties();
        if(processor==null)
            return null;

        View v = View.inflate(context, R.layout.view_session_list_session, null);

        TextView tv_name = v.findViewById(R.id.tv_name);
        TextView tv_msg = v.findViewById(R.id.tv_msg);
        TextView tv_time = v.findViewById(R.id.tv_time);
        TextView tv_remarks=v.findViewById(R.id.tv_remarks);

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
            tv_name.setText(userInfoEntity.getName()+sessionProperties.getMainNameEndWith());
        else {
            tv_name.setText("未知用户");
        }
        if(sessionProperties.getSessionTextColor()!=-1)
            tv_name.setTextColor(sessionProperties.getSessionTextColor());

        if(sessionEntity.isDisabled()){
            tv_name.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        }

        String remarks=sessionEntity.getParamsInMap().get("remarks");
        if(TextUtils.isEmpty(remarks)){
           tv_remarks.setText("无备注");
        }else{
            tv_remarks.setText("备注:"+remarks);
        }


        return v;
    }
}
