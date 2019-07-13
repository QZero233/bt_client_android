package com.nasa.bt;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.utils.LocalSettingsUtils;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Handler mainAuthReportHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            Map<String,String> params=datagram.getParamsAsString();
            ActionReport actionReport= JSON.parseObject(params.get("action_report"),ActionReport.class);

            if(!actionReport.getActionIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_SIGN_IN))
                return;

            if(actionReport.getActionStatus().equals("0")){
                //验证失败，断线，跳转
                MessageLoop.processDatagram(new Datagram(LoopResource.INBOX_IDENTIFIER_DISCONNECTED,null));
                startActivity(new Intent(MainActivity.this,AuthInfoActivity.class));
                Toast.makeText(MainActivity.this,"身份验证失败，请输入身份验证信息",Toast.LENGTH_SHORT).show();
            }else{
                //验证成功，拉取信息
                setTitle("正在更新信息......");

                Datagram getMessageDatagram = new Datagram(Datagram.IDENTIFIER_GET_MESSAGE_INDEX, null);
                LoopResource.sendDatagram(getMessageDatagram);

                Datagram getSessionDatagram=new Datagram(Datagram.IDENTIFIER_GET_SESSIONS_INDEX, null);
                LoopResource.sendDatagram(getSessionDatagram);

                //TODO 等待更新拉取完成，现在暂时无法得知何时完成
                startActivity(new Intent(MainActivity.this,SessionListActivity.class));
                finish();
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 操作顺序：
         * 0.启动服务，连接服务器
         * 1.身份验证，不通过则断线，并跳转到身份信息设置窗口，在那个窗口里发消息重连
         * 2.拉取更新，目前版本只拉取所有Session和未读消息
         * 3.通过，启动SessionListActivity，finish
         */

        LocalDatabaseHelper.reset(this);
        KeyUtils.initContext(this);

        setTitle("正在连接服务器.......");
        startService(new Intent(this, MessageLoopService.class));

        MessageIntent messageIntent=new MessageIntent("MAIN_AUTH_REPORT",Datagram.IDENTIFIER_REPORT,mainAuthReportHandler,0,1);
        MessageLoop.addIntent(messageIntent);

        String name = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_NAME);
        String code = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_CODE_HASH);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(code)) {
            startActivity(new Intent(this, AuthInfoActivity.class));
            Toast.makeText(this, "请设置基本信息", Toast.LENGTH_SHORT).show();
            MessageLoop.processDatagram(new Datagram(LoopResource.INBOX_IDENTIFIER_DISCONNECTED,null));
        }


    }

    @Override
    protected void onRestart() {
        super.onRestart();
        LocalDatabaseHelper.reset(this);
        setTitle("正在连接服务器.......");
        startService(new Intent(this, MessageLoopService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageLoop.removeIntent(Datagram.IDENTIFIER_REPORT,"MAIN_AUTH_REPORT",1);
    }
}
