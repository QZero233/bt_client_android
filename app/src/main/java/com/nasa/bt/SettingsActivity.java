package com.nasa.bt;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle("设置");
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

                MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT);

                finish();
            }
        });
        builder.show();
    }

    public void resetRSAKeySet(View v){
        try {
            KeyUtils utils = KeyUtils.getInstance();
            utils.genKeySet();
            utils.saveKeySet();

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

}
