package com.nasa.bt;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;

import java.util.HashMap;
import java.util.Map;

public class UserDetailActivity extends AppCompatActivity {

    private ProgressBar pb;
    private UserInfoEntity userInfoEntity;
    private TextView tv_name,tv_uid;
    private Handler sessionReportHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            ActionReport report= JSON.parseObject(params.get("action_report"),ActionReport.class);
            if(report==null || !report.getActionIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_CREATE_SESSION))
                return;

            Map<String,String> paramsGet=new HashMap<>();
            paramsGet.put("session_id",report.getMore());
            Datagram datagramGet=new Datagram(Datagram.IDENTIFIER_GET_SESSION_DETAIL,paramsGet,null);
            LoopResource.sendDatagram(datagramGet);

            Toast.makeText(UserDetailActivity.this,"创建成功，正在向服务器请求会话信息",Toast.LENGTH_SHORT).show();
        }
    };
    private Handler sessionDetailHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

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

        MessageLoop.addIntent(new MessageIntent("USER_DETAIL_SESSION_REPORT",Datagram.IDENTIFIER_REPORT,sessionReportHandler,0,1));
        MessageLoop.addIntent(new MessageIntent("USER_DETAIL_SESSION_DETAIL",Datagram.IDENTIFIER_RETURN_SESSION_DETAIL,sessionDetailHandler,0,1));
    }

    public void createNormalChatSession(View v){
        Map<String,String> params=new HashMap<>();
        params.put("session_type",String.valueOf(SessionEntity.TYPE_NORMAL));
        params.put("uid_dst", userInfoEntity.getId());
        params.put("params","");
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_CREATE_SESSION,params,null);
        LoopResource.sendDatagram(datagram);
        pb.setVisibility(View.VISIBLE);
    }

    public void createSecretChatSession(View view){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);

        View v=View.inflate(this,R.layout.view_create_secret_chat,null);
        final TextInputEditText et_key;
        et_key=v.findViewById(R.id.et_key);

        builder.setTitle("创建私密聊天");
        builder.setCancelable(false);
        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("创建", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String key=et_key.getText().toString();

                if(TextUtils.isEmpty(key)){
                    Toast.makeText(UserDetailActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                String keyHash= SHA256Utils.getSHA256InHex(key);

                Map<String,String> sessionParam=new HashMap<>();
                sessionParam.put("key",keyHash);

                Map<String,String> params=new HashMap<>();
                params.put("session_type",String.valueOf(SessionEntity.TYPE_SECRET_CHAT));
                params.put("uid_dst", userInfoEntity.getId());
                params.put("params",JSON.toJSONString(sessionParam));
                Datagram datagram=new Datagram(Datagram.IDENTIFIER_CREATE_SESSION,params,null);
                LoopResource.sendDatagram(datagram);
                pb.setVisibility(View.VISIBLE);
            }
        });
        builder.setView(v);
        builder.show();
    }
}
