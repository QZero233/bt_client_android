package com.nasa.bt.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.nasa.bt.MessageReadActivity;
import com.nasa.bt.R;
import com.nasa.bt.ca.CABasic;
import com.nasa.bt.ca.CAObject;
import com.nasa.bt.ca.CAUtils;
import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.crypt.AppKeyStore;
import com.nasa.bt.crypt.SHA256Utils;

public class GenCAFragment extends Fragment {

    private TextInputEditText et_ip,et_key;
    private Context context;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context=getActivity();

        View v=inflater.inflate(R.layout.fragment_ca_gen,container,false);

        et_ip=v.findViewById(R.id.et_ip);
        et_key=v.findViewById(R.id.et_key);
        Button btn_gen=v.findViewById(R.id.btn_gen);
        btn_gen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gen();
            }
        });

        return v;
    }

    public void gen(){
        String ip=et_ip.getText().toString();
        if(TextUtils.isEmpty(ip))
            ip=null;

        String pubKey=et_key.getText().toString();
        if(TextUtils.isEmpty(pubKey)){
            Toast.makeText(context,"公钥不能为空",Toast.LENGTH_SHORT).show();
            return;
        }
        String serverPubKeyHash= SHA256Utils.getSHA256InHex(pubKey);

        long endTime=0;//TODO 加入日期时间选择框

        RSAKeySet keySet= AppKeyStore.getInstance().getCurrentKeySet();
        if(keySet==null){
            Toast.makeText(context,"本地密钥对加载失败",Toast.LENGTH_SHORT).show();
            return;
        }

        String localPubKeyHash=SHA256Utils.getSHA256InHex(keySet.getPub());

        CABasic caBasic=new CABasic(ip,serverPubKeyHash,endTime,localPubKeyHash);
        CAObject caObject= CAUtils.genCA(caBasic,keySet);
        if(caObject==null){
            Toast.makeText(context,"生成失败",Toast.LENGTH_SHORT).show();
            return;
        }else{
            Toast.makeText(context,"生成成功（长按可复制）",Toast.LENGTH_SHORT).show();

            Intent intent=new Intent(context, MessageReadActivity.class);
            intent.putExtra("message",CAUtils.caObjectToString(caObject));
            startActivity(intent);

            getActivity().finish();
        }

    }

}
