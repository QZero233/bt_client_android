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

import com.nasa.bt.contract.SessionListContract;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.SessionEntityForShow;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.presenter.SessionListPresenter;
import com.nasa.bt.session.JoinSessionCallback;
import com.nasa.bt.session.SessionProcessor;
import com.nasa.bt.session.SessionProcessorFactory;
import com.nasa.bt.session.SessionProperties;
import com.nasa.bt.upgrade.UpgradeStatus;
import com.nasa.bt.utils.TimeUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 心中有党，成绩理想
 */
public class SessionListActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemLongClickListener,
        NavigationView.OnNavigationItemSelectedListener, SessionListContract.View {

    private ListView lv_sessions;
    private SwipeRefreshLayout sl_main;
    private TextView tv_name;
    private DrawerLayout dl;

    private List<SessionEntityForShow> sessionEntities;

    private SessionListPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        presenter=new SessionListPresenter(this);
        presenter.attachView(this);
        presenter.startListening();
        presenter.doSync();

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

        presenter.reloadSessionList();
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
            case R.id.m_key_set:
                startActivity(new Intent(this,RSAKeyManagerActivity.class));
                break;
            case R.id.m_ca:
                startActivity(new Intent(this,CACenterActivity.class));
                break;
            case R.id.m_key_set_connection:
                Intent intent=new Intent(this,MessageReadActivity.class);
                intent.putExtra("message", KeyUtils.read().getPub());
                startActivity(intent);
                break;
        }
        return false;
    }

    private void refresh(){
        presenter.doRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        presenter.stopListening();
        presenter.detachView();
    }

    @Override
    public void changeRefreshStatus(boolean isRefreshing) {
        sl_main.setRefreshing(isRefreshing);
    }

    @Override
    public void reloadSessionList(List<SessionEntityForShow> sessionEntityList) {
        this.sessionEntities=sessionEntityList;
        lv_sessions.setAdapter(new SessionListAdapter(sessionEntities,this));
    }

    @Override
    public void setDrawerHeadInfo(String name) {
        if(name==null)
            tv_name.setText("正在加载.....");
        else
            tv_name.setText(name);
    }

    @Override
    public void showUpgradeInfo(final UpgradeStatus upgradeStatus) {
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
        }
    }

    @Override
    public void onConnectionStatusChanged(int status) {
        switch (status){
            case MessageLoopService.STATUS_CONNECTED:
                setTitle("已连接");
                break;
            case MessageLoopService.STATUS_CONNECTING:
                setTitle("正在连接服务器");
                break;
            case MessageLoopService.STATUS_DISCONNECTED:
                setTitle("已断线");
                break;
        }
    }

    @Override
    public void onSyncFailure() {
        showToast("因未知原因同步失败");
        setTitle("同步失败");
    }

    @Override
    public void onSyncSuccess() {

    }

    @Override
    public void onRefreshFailure() {
        showToast("因未知原因刷新失败");
        setTitle("刷新失败");
    }

    @Override
    public void onRefreshSuccess() {
        if (sl_main.isRefreshing()) {
            sl_main.setRefreshing(false);
            Toast.makeText(SessionListActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAuthFailed() {
        startActivity(new Intent(SessionListActivity.this,AuthInfoActivity.class));
        finish();
    }

    private void startChat(final int index) {
        final SessionEntity sessionEntity = sessionEntities.get(index).getSessionEntity();
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
        presenter.reloadSessionList();
    }

    @Override
    public void onRefresh() {
        refresh();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(this, SessionDetailActivity.class);
        intent.putExtra("sessionEntity", sessionEntities.get(i).getSessionEntity());
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


    @Override
    public void showToast(String content) {
        Toast.makeText(this,content,Toast.LENGTH_SHORT).show();
    }
}

class SessionListAdapter extends BaseAdapter {

    private List<SessionEntityForShow> sessionEntities;
    private Context context;

    public SessionListAdapter(List<SessionEntityForShow> sessionEntities, Context context) {
        this.sessionEntities = sessionEntities;
        this.context = context;
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
        SessionEntityForShow sessionEntityForShow = sessionEntities.get(i);

        SessionEntity sessionEntity=sessionEntityForShow.getSessionEntity();
        UserInfoEntity userInfoEntity=sessionEntityForShow.getUserInfoEntity();
        int unreadMessageCount=sessionEntityForShow.getUnreadMessageCount();

        if(sessionEntity==null || userInfoEntity==null){
            View v=new View(context);
            return v;
        }

        SessionProcessor processor=SessionProcessorFactory.getProcessor(sessionEntity.getSessionType());
        SessionProperties sessionProperties=processor.getSessionProperties();
        if(processor==null)
            return null;

        View v = View.inflate(context, R.layout.view_session_list_session, null);

        TextView tv_name = v.findViewById(R.id.tv_name);
        TextView tv_msg = v.findViewById(R.id.tv_msg);
        TextView tv_time = v.findViewById(R.id.tv_time);
        TextView tv_remarks=v.findViewById(R.id.tv_remarks);

        tv_name.setText(userInfoEntity.getName()+sessionProperties.getMainNameEndWith());

        if (unreadMessageCount==0) {
            if (TextUtils.isEmpty(sessionEntity.getLastMessage())) {
                tv_msg.setText("无消息");
                tv_time.setVisibility(View.GONE);
            } else {
                tv_msg.setText(processor.getMessageMain(sessionEntity));
                tv_time.setText(TimeUtils.toStandardTime(sessionEntity.getLastTime()));
            }
        } else {
            tv_msg.setText("有 " + unreadMessageCount + " 条未读消息");
            tv_msg.setTextColor(Color.RED);
            tv_time.setText(TimeUtils.toStandardTime(sessionEntityForShow.getLastUnreadMessage().getTime()));
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
