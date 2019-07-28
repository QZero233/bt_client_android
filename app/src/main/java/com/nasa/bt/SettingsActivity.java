package com.nasa.bt;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.nasa.bt.ca.CAUtils;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.data.LocalDatabaseHelper;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private Switch sw_ca;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle("设置");

        sw_ca=findViewById(R.id.sw_ca);
        sw_ca.setChecked(LocalSettingsUtils.readBoolean(this,LocalSettingsUtils.FIELD_FORCE_CA));
        sw_ca.setOnCheckedChangeListener(this);

        Intent intent=getIntent();
        String toast=intent.getStringExtra("toast");
        if(!TextUtils.isEmpty(toast))
            Toast.makeText(this,toast,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        LocalSettingsUtils.saveBoolean(this,LocalSettingsUtils.FIELD_FORCE_CA,b);
    }

    public void setServerIp(View v){
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
                if(TextUtils.isEmpty(newIp))
                    newIp=MessageLoopService.SERVER_IP_DEFAULT;
                LocalSettingsUtils.save(SettingsActivity.this, LocalSettingsUtils.FIELD_SERVER_IP, newIp);
                Toast.makeText(SettingsActivity.this, "修改成功", Toast.LENGTH_SHORT).show();

                LocalDatabaseHelper.reset(getApplicationContext());

                MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT);

                finish();
            }
        });
        builder.show();
    }

    public void resetRSAKeySet(View v){
        try {
            RSAKeySet rsaKeySet=KeyUtils.genKeySet();
            KeyUtils.saveKeySet(rsaKeySet);

            MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT);

            Toast.makeText(this, "重置成功", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "重置失败", Toast.LENGTH_SHORT).show();
        }
    }

    public void logOut(View v){
        LocalSettingsUtils.save(this, LocalSettingsUtils.FIELD_NAME, "");
        LocalSettingsUtils.save(this, LocalSettingsUtils.FIELD_CODE_HASH, "");
        LocalSettingsUtils.save(this, LocalSettingsUtils.FIELD_CODE_LAST, "");

        MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_DISCONNECTED);

        Toast.makeText(this, "退出成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void setClientCA(View v){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("设置客户端证书");

        final EditText et_ca=new EditText(this);

        if(CAUtils.readCAFile(this)!=null)
            et_ca.setText(CAUtils.readCAFile(this));

        builder.setView(et_ca);
        builder.setNegativeButton("取消",null).setPositiveButton("设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String caStr=et_ca.getText().toString();

                CAUtils.writeCAFile(SettingsActivity.this,caStr);
                Toast.makeText(SettingsActivity.this,"设置成功",Toast.LENGTH_SHORT).show();
                MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT);

                finish();
            }
        });
        builder.show();
    }


}
