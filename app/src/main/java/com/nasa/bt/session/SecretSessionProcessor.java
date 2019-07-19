package com.nasa.bt.session;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.nasa.bt.crypt.AESUtils;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.data.entity.SessionEntity;

import java.util.Map;

public class SecretSessionProcessor implements SessionProcessor {

    private byte[] aesKey=null;

    @Override
    public SessionProperties getSessionProperties() {
        return new SessionProperties("加密会话",Color.RED,"绝对安全通信","(加密聊天)");
    }

    @Override
    public String getMessageMain(SessionEntity sessionEntity) {
        return "加密信息，需密码解密查看";
    }

    @Override
    public void joinSession(SessionEntity sessionEntity, final JoinSessionCallback callback, final Context context) {
        Map<String, String> sessionParams = sessionEntity.getParamsInMap();
        final String sessionKeyHash = sessionParams.get("key");

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("请输入此加密聊天的密码");

        final EditText et = new EditText(context);
        et.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(et);
        builder.setNegativeButton("取消", null);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String key = et.getText().toString();
                String keyHash = SHA256Utils.getSHA256InHex(key);
                if (!keyHash.equals(sessionKeyHash)) {
                    Toast.makeText(context, "密码不正确", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Intent intent=new Intent();
                    intent.putExtra("key",key);
                    callback.start(intent);
                }
            }
        });
        builder.show();
    }

    @Override
    public String processContentSent(String content, SessionEntity sessionEntity, Intent intentWithParams) {
        if(aesKey==null)
            aesKey= AESUtils.getAESKey(intentWithParams.getStringExtra("key"));

        return AESUtils.aesEncrypt(content,aesKey);
    }

    @Override
    public String processContentGot(String content, SessionEntity sessionEntity, Intent intentWithParams) {
        if(aesKey==null)
            aesKey= AESUtils.getAESKey(intentWithParams.getStringExtra("key"));

        return AESUtils.aesDecrypt(content,aesKey);
    }
}
