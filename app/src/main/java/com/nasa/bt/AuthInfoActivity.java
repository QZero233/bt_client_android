package com.nasa.bt;

import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

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
    }

    public void confirm(View v){
        String name=et_name.getText().toString();
        String code=et_code.getText().toString();

        if(TextUtils.isEmpty(name) || TextUtils.isEmpty(code)){
            Toast.makeText(this,"请输入正确信息",Toast.LENGTH_SHORT).show();
            return;
        }

        LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_NAME,name);
        LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_CODE_HASH,code);//TODO 取hash
        LocalSettingsUtils.save(this,LocalSettingsUtils.FIELD_SID,"");
        Toast.makeText(this,"设置成功，正在尝试重连",Toast.LENGTH_SHORT).show();
//        MessageLoopService.instance.needReConnect=true;
        finish();
    }

}
