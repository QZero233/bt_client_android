package com.nasa.bt;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.loop.ActionReportListener;
import com.nasa.bt.loop.DatagramListener;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;

import java.util.HashMap;
import java.util.Map;

public class UserDetailActivity extends AppCompatActivity {

    private ProgressBar pb;
    private UserInfoEntity userInfoEntity;
    private TextView tv_name,tv_uid;


    private ActionReportListener createSessionReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_SESSION_DETAIL,new ParamBuilder().putParam("session_id",actionReport.getMore()).build());
            SendDatagramUtils.sendDatagram(datagramGet);

            Toast.makeText(UserDetailActivity.this,"创建成功，正在向服务器请求会话信息",Toast.LENGTH_SHORT).show();
        }
    };

    private DatagramListener sessionDetailListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            Toast.makeText(UserDetailActivity.this,"会话创建成功",Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        userInfoEntity = (UserInfoEntity) getIntent().getSerializableExtra("user");
        if(userInfoEntity ==null){
            finish();
            return;
        }

        tv_name=findViewById(R.id.tv_name);
        tv_uid=findViewById(R.id.tv_uid);
        pb=findViewById(R.id.pb);

        tv_name.setText(userInfoEntity.getName());
        tv_uid.setText(userInfoEntity.getId());

        MessageLoopUtils.registerActionReportListenerNormal("USER_DETAIL_SESSION_REPORT",Datagram.IDENTIFIER_CREATE_SESSION,createSessionReportListener);
        MessageLoopUtils.registerListenerNormal("USER_DETAIL_SESSION_DETAIL",Datagram.IDENTIFIER_SESSION_DETAIL,sessionDetailListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageLoopUtils.unregisterListener("USER_DETAIL_SESSION_REPORT");
        MessageLoopUtils.unregisterListener("USER_DETAIL_SESSION_DETAIL");
    }

    public void createNormalChatSession(View v){
        ParamBuilder paramBuilder=new ParamBuilder();
        paramBuilder.putParam("session_type",String.valueOf(SessionEntity.TYPE_NORMAL)).putParam("uid_dst", userInfoEntity.getId()).putParam("params","");
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_CREATE_SESSION,paramBuilder.build());
        SendDatagramUtils.sendDatagram(datagram);
        pb.setVisibility(View.VISIBLE);
    }

    public void createSecretChatSession(View view){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);

        View v=View.inflate(this,R.layout.view_create_secret_chat,null);
        final TextInputEditText et_key,et_remarks;
        et_key=v.findViewById(R.id.et_key);
        et_remarks=v.findViewById(R.id.et_remarks);

        builder.setTitle("创建私密聊天");
        builder.setCancelable(false);
        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("创建", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String key=et_key.getText().toString();
                String remarks=et_remarks.getText().toString();

                if(TextUtils.isEmpty(remarks))
                    remarks="私密聊天";

                if(TextUtils.isEmpty(key)){
                    Toast.makeText(UserDetailActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                String keyHash= SHA256Utils.getSHA256InHex(key);

                Map<String,String> sessionParam=new HashMap<>();
                sessionParam.put("key",keyHash);
                sessionParam.put("remarks",remarks);

                ParamBuilder paramBuilder=new ParamBuilder();
                paramBuilder.putParam("session_type",String.valueOf(SessionEntity.TYPE_SECRET_CHAT)).putParam("uid_dst", userInfoEntity.getId()).putParam("params",JSON.toJSONString(sessionParam));
                Datagram datagram=new Datagram(Datagram.IDENTIFIER_CREATE_SESSION,paramBuilder.build());
                SendDatagramUtils.sendDatagram(datagram);
                pb.setVisibility(View.VISIBLE);
            }
        });
        builder.setView(v);
        builder.show();
    }
}
