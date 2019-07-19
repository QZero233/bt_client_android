package com.nasa.bt;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.loop.MessageLoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.NotificationUtils;

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
                MessageLoop.processDatagram(new Datagram(MessageLoopResource.INBOX_IDENTIFIER_DISCONNECTED,null));
                startActivity(new Intent(MainActivity.this,AuthInfoActivity.class));
                Toast.makeText(MainActivity.this,"身份验证失败，请输入身份验证信息",Toast.LENGTH_SHORT).show();
            }else{
                pullUpdate();
            }

        }
    };

    private Handler mainRefreshReportHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Datagram datagram= (Datagram) msg.obj;
            ActionReport actionReport=JSON.parseObject(datagram.getParamsAsString().get("action_report"),ActionReport.class);
            if(!actionReport.getActionIdentifier().equalsIgnoreCase(Datagram.IDENTIFIER_REFRESH))
                return;

            onRefreshOver();
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
         * 2.向服务器申请刷新
         * 3.通过，启动SessionListActivity，finish
         */

        //TODO 在程序打开后清除所有通知

        String name = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_NAME);
        String code = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_CODE_HASH);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(code)) {
            startActivity(new Intent(this, AuthInfoActivity.class));
            Toast.makeText(this, "请设置基本信息", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LocalDatabaseHelper.reset(this);
        KeyUtils.initContext(this);

        setTitle("正在连接服务器.......");
        startService(new Intent(this, MessageLoopService.class));

        MessageIntent messageIntentAuth=new MessageIntent("MAIN_AUTH_REPORT",Datagram.IDENTIFIER_REPORT,mainAuthReportHandler,0,1);
        MessageIntent messageIntentRefresh=new MessageIntent("MAIN_REFRESH_REPORT",Datagram.IDENTIFIER_REPORT,mainRefreshReportHandler,0,1);
        MessageLoop.addIntent(messageIntentAuth);
        MessageLoop.addIntent(messageIntentRefresh);

        pullUpdate();
    }


    private void pullUpdate(){
        setTitle("正在更新信息......");

        Datagram datagram=new Datagram(Datagram.IDENTIFIER_REFRESH,null);
        MessageLoopResource.sendDatagram(datagram);
    }

    private void onRefreshOver(){
        startActivity(new Intent(MainActivity.this,SessionListActivity.class));
        finish();
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
                    if (TextUtils.isEmpty(newIp))
                        newIp = MessageLoopService.SERVER_IP_DEFAULT;
                    LocalSettingsUtils.save(MainActivity.this, LocalSettingsUtils.FIELD_SERVER_IP, newIp);
                    Toast.makeText(MainActivity.this, "修改成功", Toast.LENGTH_SHORT).show();

                    Datagram datagram = new Datagram(MessageLoopResource.INBOX_IDENTIFIER_RECONNECT, null);
                    MessageLoop.processDatagram(datagram);

                    finish();
                }
            });
            builder.show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageLoop.removeIntent(Datagram.IDENTIFIER_REPORT,"MAIN_AUTH_REPORT",1);
        MessageLoop.removeIntent(Datagram.IDENTIFIER_REPORT,"MAIN_REFRESH_REPORT",1);
    }
}
