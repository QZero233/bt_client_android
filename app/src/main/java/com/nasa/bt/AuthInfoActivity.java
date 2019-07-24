package com.nasa.bt;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.nasa.bt.cls.ActionReport;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.loop.ActionReportListener;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

public class AuthInfoActivity extends AppCompatActivity {

    private TextInputEditText et_name,et_code;
    private ProgressBar pb;

    private ActionReportListener authReportListener=new ActionReportListener() {
        @Override
        public void onActionReportReach(ActionReport actionReport) {
            pb.setVisibility(View.GONE);
            if(actionReport.getActionStatusInBoolean()){
                //身份验证成功
                Toast.makeText(AuthInfoActivity.this,"身份验证成功",Toast.LENGTH_SHORT).show();
                startActivity(new Intent(AuthInfoActivity.this,SessionListActivity.class));
                finish();
            }else{
                //身份验证失败
                Toast.makeText(AuthInfoActivity.this,"身份验证失败，请检查输入信息是否有误",Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_info);

        et_name=findViewById(R.id.et_name);
        et_code=findViewById(R.id.et_code);
        pb=findViewById(R.id.pb);

        String nameLast=LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_NAME_LAST);
        String codeLast=LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_CODE_LAST);
        if(!TextUtils.isEmpty(nameLast))
            et_name.setText(nameLast);
        if(!TextUtils.isEmpty(codeLast))
            et_code.setText(codeLast);
    }

    public void confirm(View v){
        String name=et_name.getText().toString();
        String code=et_code.getText().toString();

        if(TextUtils.isEmpty(name) || TextUtils.isEmpty(code)){
            Toast.makeText(this,"请输入正确信息",Toast.LENGTH_SHORT).show();
            return;
        }

        LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_CODE_LAST,code);
        LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_NAME_LAST,name);

        code= SHA256Utils.getSHA256InHex(code);

        LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_NAME,name);
        LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_CODE_HASH,code);
        Toast.makeText(this,"设置成功，正在尝试重连",Toast.LENGTH_SHORT).show();

        MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT);

        pb.setVisibility(View.VISIBLE);
        MessageLoopUtils.registerSpecifiedTimesActionReportListener("AUTH_INFO_AUTH_REPORT",Datagram.IDENTIFIER_SIGN_IN,1,authReportListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_auth, menu);
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
                    LocalSettingsUtils.save(AuthInfoActivity.this, LocalSettingsUtils.FIELD_SERVER_IP, newIp);
                    Toast.makeText(AuthInfoActivity.this, "修改成功", Toast.LENGTH_SHORT).show();

                    MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT);
                }
            });
            builder.show();
        }

        return super.onOptionsItemSelected(item);
    }

}
