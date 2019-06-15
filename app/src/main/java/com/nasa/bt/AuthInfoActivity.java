package com.nasa.bt;

import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.loop.MessageLoopService;
import com.nasa.bt.utils.LocalSettingsUtils;

public class AuthInfoActivity extends AppCompatActivity {

    private TextInputEditText et_name,et_code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_info);

        et_name=findViewById(R.id.et_name);
        et_code=findViewById(R.id.et_code);

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

        Datagram datagramReconnect=new Datagram(LoopResource.INBOX_IDENTIFIER_RECONNECT,null);
        MessageLoop.processDatagram(datagramReconnect);

        finish();
    }

}
