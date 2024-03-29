package com.nasa.bt;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.nasa.bt.loop.ActionReportListener;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.session.JoinSessionCallback;
import com.nasa.bt.session.SessionProcessor;
import com.nasa.bt.session.SessionProcessorFactory;
import com.nasa.bt.utils.LocalSettingsUtils;

import java.util.Map;

public class SessionDetailActivity extends AppCompatActivity {

    private SessionEntity sessionEntity;
    private SessionProcessor processor;

    private SessionDao sessionDao;
    private TextView tv_name,tv_type,tv_remarks;
    private ProgressBar pb;


    private ActionReportListener deleteActionReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            pb.setVisibility(View.GONE);
            if (actionReport.getActionStatus().equalsIgnoreCase(ActionReport.STATUS_SUCCESS)) {
                Toast.makeText(SessionDetailActivity.this, "关闭成功", Toast.LENGTH_SHORT).show();
                sessionDao.setSessionDisabled(sessionEntity.getSessionId());
                finish();
            } else {
                Toast.makeText(SessionDetailActivity.this, "关闭失败", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private ActionReportListener updateActionReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            pb.setVisibility(View.GONE);
            if (actionReport.getActionStatus().equalsIgnoreCase(ActionReport.STATUS_SUCCESS)) {
                Toast.makeText(SessionDetailActivity.this, "更改成功", Toast.LENGTH_SHORT).show();
                sessionDao.addOrUpdateSession(sessionEntity);
                tv_remarks.setText(sessionEntity.getSpecifiedParam("remarks"));
            } else {
                Toast.makeText(SessionDetailActivity.this, "更改失败", Toast.LENGTH_SHORT).show();
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
        tv_remarks=findViewById(R.id.tv_remarks);
        pb=findViewById(R.id.pb);

        processor= SessionProcessorFactory.getProcessor(sessionEntity.getSessionType());

        String uidDst=sessionEntity.getIdOfOther(LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_UID));
        UserInfoDao userInfoDao=new UserInfoDao(this);
        sessionDao=new SessionDao(this);
        UserInfoEntity userInfoEntity=userInfoDao.getUserInfoById(uidDst);

        tv_name.setText(userInfoEntity.getName());
        tv_type.setText(processor.getSessionProperties().getSessionName());

        String remarks=sessionEntity.getParamsInMap().get("remarks");
        if(remarks==null)
            remarks="";
        tv_remarks.setText(remarks);


        if(!sessionEntity.isDisabled()){
            Button btn_clean=findViewById(R.id.btn_clean);
            btn_clean.setText("清空聊天记录");
        }else{
            Button btn_update=findViewById(R.id.btn_update);
            btn_update.setVisibility(View.GONE);
            Button btn_close=findViewById(R.id.btn_close);
            btn_close.setVisibility(View.GONE);
        }


        MessageLoopUtils.registerActionReportListenerNormal("SESSION_DETAIL_DELETE_REPORT",Datagram.IDENTIFIER_DELETE_SESSION,deleteActionReportListener);
        MessageLoopUtils.registerActionReportListenerNormal("SESSION_DETAIL_UPDATE_REPORT",Datagram.IDENTIFIER_UPDATE_SESSION,updateActionReportListener);
    }

    public void close(View v){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("是否关闭");
        builder.setMessage("操作不可逆，会保留本地聊天记录");
        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("关闭", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Datagram datagram=new Datagram(Datagram.IDENTIFIER_DELETE_SESSION, new ParamBuilder().putParam("session_id",sessionEntity.getSessionId()).build());
                SendDatagramUtils.sendDatagram(datagram);
                pb.setVisibility(View.VISIBLE);
            }
        });
        builder.show();
    }

    public void clean(View v){

        final MessageDao messageDao=new MessageDao(this);

        if(sessionEntity.isDisabled()){
            //删除本地会话
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("确认删除会话并清空聊天记录");
            builder.setMessage("操作不可逆");
            builder.setNegativeButton("取消", null);
            builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (messageDao.deleteAllMessage(sessionEntity.getSessionId()) && sessionDao.deleteSession(sessionEntity.getSessionId())) {
                        Toast.makeText(SessionDetailActivity.this, "操作成功", Toast.LENGTH_SHORT).show();
                        finish();
                    } else
                        Toast.makeText(SessionDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show();

                }
            });
            builder.show();
        }else{
            //清空聊天记录
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("确认清除聊天记录");
            builder.setMessage("操作不可逆");
            builder.setNegativeButton("取消", null);
            builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (messageDao.deleteAllMessage(sessionEntity.getSessionId())) {
                        Toast.makeText(SessionDetailActivity.this, "操作成功", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(SessionDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show();

                }
            });
            builder.show();
        }
    }

    public void updateDetail(View view){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);

        final EditText et_remarks=new EditText(this);
        String remarks=sessionEntity.getSpecifiedParam("remarks");
        if(remarks==null)
            remarks="";
        et_remarks.setText(remarks);

        builder.setTitle("请输入新备注");
        builder.setView(et_remarks);
        builder.setCancelable(false);

        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("更改", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String remarks=et_remarks.getText().toString();

                Map<String,String> params=sessionEntity.getParamsInMap();
                params.put("remarks",remarks);
                sessionEntity.setParamsInMap(params);

                Datagram datagram=new Datagram(Datagram.IDENTIFIER_UPDATE_SESSION,new ParamBuilder().putParam("session_id",sessionEntity.getSessionId()).
                        putParam("params",sessionEntity.getParams()).build());
                SendDatagramUtils.sendDatagram(datagram);
                Toast.makeText(SessionDetailActivity.this,"正在更改......",Toast.LENGTH_SHORT).show();
                pb.setVisibility(View.VISIBLE);
            }
        });

        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageLoopUtils.unregisterListener("SESSION_DETAIL_DELETE_REPORT");
        MessageLoopUtils.unregisterListener("SESSION_DETAIL_UPDATE_REPORT");
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
