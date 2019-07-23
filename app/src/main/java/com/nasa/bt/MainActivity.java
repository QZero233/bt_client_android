package com.nasa.bt;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.loop.ActionReportListener;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.upgrade.UpgradeStatus;
import com.nasa.bt.upgrade.UpgradeUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

public class MainActivity extends AppCompatActivity {

    private ActionReportListener mainAuthReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            if(actionReport.getActionStatus().equals("0")){
                //验证失败，断线，跳转
                MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_DISCONNECTED);
                startActivity(new Intent(MainActivity.this,AuthInfoActivity.class));
                Toast.makeText(MainActivity.this,"身份验证失败，请输入身份验证信息",Toast.LENGTH_SHORT).show();
            }
        }
    };

    private ActionReportListener mainRefreshReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            onRefreshOver();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 操作顺序：
         * 0.检查更新，启动服务，连接服务器
         * 1.身份验证，不通过则断线，并跳转到身份信息设置窗口，在那个窗口里发消息重连
         * 2.向服务器申请刷新
         * 3.通过，启动SessionListActivity，finish
         */
        checkUpgrade();
    }


    private void checkUpgrade(){
        setTitle("正在检查更新");
        final Handler handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what==1){
                    //有更新
                    final UpgradeStatus upgradeStatus= (UpgradeStatus) msg.obj;
                    AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
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
                }else
                    doMain();
            }
        };
        UpgradeUtils.checkUpgrade(getApplicationContext(),handler);
    }

    private void doMain(){
        String name = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_NAME);
        String code = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_CODE_HASH);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(code)) {
            startActivity(new Intent(this, AuthInfoActivity.class));
            Toast.makeText(this, "请设置基本信息", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LocalDatabaseHelper.reset(this);

        setTitle("正在连接服务器.......");
        startService(new Intent(this, MessageLoopService.class));

        MessageLoopUtils.registerActionReportListenerNormal("MAIN_AUTH_REPORT",Datagram.IDENTIFIER_SIGN_IN,mainAuthReportListener);

        pullUpdate();
    }


    private void pullUpdate(){
        setTitle("正在更新信息......");

        MessageLoopUtils.registerSpecifiedTimesActionReportListener("MAIN_REFRESH_REPORT",Datagram.IDENTIFIER_REFRESH,1,mainRefreshReportListener);
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_REFRESH,null);
        SendDatagramUtils.sendDatagram(datagram);
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

                    MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT);

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

        MessageLoopUtils.unregisterListener("MAIN_REFRESH_REPORT");
        MessageLoopUtils.unregisterListener("MAIN_AUTH_REPORT");
    }


}
