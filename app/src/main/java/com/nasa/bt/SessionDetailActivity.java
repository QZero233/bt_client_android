package com.nasa.bt;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.loop.MessageLoopResource;
import com.nasa.bt.session.JoinSessionCallback;
import com.nasa.bt.session.SessionProcessor;
import com.nasa.bt.session.SessionProcessorFactory;
import com.nasa.bt.utils.LocalSettingsUtils;

public class SessionDetailActivity extends AppCompatActivity {

    private SessionEntity sessionEntity;
    private SessionProcessor processor;

    private MessageDao messageDao;
    private SessionDao sessionDao;
    private TextView tv_name,tv_type;
    private ProgressBar pb;

    private Handler deleteReportHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            ActionReport actionReport= JSON.parseObject(datagram.getParamsAsString().get("action_report"),ActionReport.class);
            if(!actionReport.getActionIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_DELETE_SESSION))
                return;

            pb.setVisibility(View.GONE);
            if(actionReport.getActionStatus().equalsIgnoreCase(ActionReport.STATUS_SUCCESS)){
                Toast.makeText(SessionDetailActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
                //TODO 不删除本地消息，不删除本地会话，把类型设置为disabled
                sessionDao.deleteSession(sessionEntity.getSessionId());
                messageDao.deleteAllMessage(sessionEntity.getSessionId());
                finish();
            }else{
                Toast.makeText(SessionDetailActivity.this,"删除失败",Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        sessionEntity= (SessionEntity) getIntent().getSerializableExtra("sessionEntity");
        if(sessionEntity==null){
            finish();
            return;
        }

        tv_name=findViewById(R.id.tv_name);
        tv_type=findViewById(R.id.tv_type);
        pb=findViewById(R.id.pb);

        processor= SessionProcessorFactory.getProcessor(sessionEntity.getSessionType());

        String uidDst=sessionEntity.getIdOfOther(LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_UID));
        UserInfoDao userInfoDao=new UserInfoDao(this);
        messageDao=new MessageDao(this);
        sessionDao=new SessionDao(this);
        UserInfoEntity userInfoEntity=userInfoDao.getUserInfoById(uidDst);

        tv_name.setText(userInfoEntity.getName());
        tv_type.setText(processor.getName());

        MessageLoop.addIntent(new MessageIntent("SESSION_DETAIL_DELETE_REPORT", Datagram.IDENTIFIER_REPORT,deleteReportHandler,0,1));
    }

    public void delete(View v){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("是否删除");
        builder.setMessage("操作不可逆，这将会清除本地聊天记录");
        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Datagram datagram=new Datagram(Datagram.IDENTIFIER_DELETE_SESSION, new ParamBuilder().putParam("session_id",sessionEntity.getSessionId()).build());
                MessageLoopResource.sendDatagram(datagram);
                pb.setVisibility(View.VISIBLE);
            }
        });
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageLoop.removeIntent(Datagram.IDENTIFIER_REPORT,"SESSION_DETAIL_DELETE_REPORT",1);
    }

    public void join(View v){
        if(processor==null)
            return;

        JoinSessionCallback callback=new JoinSessionCallback() {
            @Override
            public void start(Intent intentWithParams) {
                if(intentWithParams ==null)
                    intentWithParams=new Intent();
                intentWithParams.putExtra("sessionEntity",sessionEntity);
                intentWithParams.setClass(SessionDetailActivity.this,ChatActivity.class);
                startActivity(intentWithParams);
                finish();
            }
        };

        processor.joinSession(sessionEntity,callback,this);
    }
}
