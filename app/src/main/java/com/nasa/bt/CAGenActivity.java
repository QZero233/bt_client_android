package com.nasa.bt;

import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.nasa.bt.ca.CABasic;
import com.nasa.bt.ca.CAObject;
import com.nasa.bt.ca.CAUtils;
import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.crypt.AppKeyStore;
import com.nasa.bt.crypt.SHA256Utils;

public class CAGenActivity extends AppCompatActivity {

    private TextInputEditText et_ip,et_key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cagen);

        et_ip=findViewById(R.id.et_ip);
        et_key=findViewById(R.id.et_key);
    }

    public void gen(View v){
        String ip=et_ip.getText().toString();
        if(TextUtils.isEmpty(ip))
            ip=null;

        String pubKey=et_key.getText().toString();
        if(TextUtils.isEmpty(pubKey)){
            Toast.makeText(this,"公钥不能为空",Toast.LENGTH_SHORT).show();
            return;
        }
        String serverPubKeyHash= SHA256Utils.getSHA256InHex(pubKey);

        long endTime=0;//TODO 加入日期时间选择框

        RSAKeySet keySet= AppKeyStore.getInstance().getCurrentKeySet();
        if(keySet==null){
            Toast.makeText(this,"本地密钥对加载失败",Toast.LENGTH_SHORT).show();
            return;
        }

        String localPubKeyHash=SHA256Utils.getSHA256InHex(keySet.getPub());

        CABasic caBasic=new CABasic(ip,serverPubKeyHash,endTime,localPubKeyHash);
        CAObject caObject= CAUtils.genCA(caBasic,keySet);
        if(caObject==null){
            Toast.makeText(this,"生成失败",Toast.LENGTH_SHORT).show();
            return;
        }else{
            Toast.makeText(this,"生成成功（长按可复制）",Toast.LENGTH_SHORT).show();

            Intent intent=new Intent(this,MessageReadActivity.class);
            intent.putExtra("message",CAUtils.caObjectToString(caObject));
            startActivity(intent);
            finish();
        }

    }

}
