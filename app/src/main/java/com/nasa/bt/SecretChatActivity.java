package com.nasa.bt;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.SecretChat;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.crypt.AESUtils;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.utils.CommonDbHelper;
import com.nasa.bt.utils.LocalDbUtils;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.TimeUtils;
import com.nasa.bt.utils.UUIDUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecretChatActivity extends AppCompatActivity {

    private ListView lv_msg;
    private EditText et_msg;
    private SecretChat secretChat;

    private String key;
    private String dstUid;

    private CommonDbHelper userInfoHelper,msgHelper;

    private Handler changedHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            refresh();
            markRead();
        }
    };

    private MessageIntent intentReport=new MessageIntent(UUIDUtils.getRandomUUID(), Datagram.IDENTIFIER_REPORT,changedHandler,0,1);
    private MessageIntent intentMessage=new MessageIntent(UUIDUtils.getRandomUUID(), Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,changedHandler,0,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        lv_msg=findViewById(R.id.lv_msg);
        et_msg=findViewById(R.id.et_msg);

        userInfoHelper= LocalDbUtils.getUserInfoHelper(this);
        msgHelper=LocalDbUtils.getMsgHelper(this);

        secretChat= (SecretChat) getIntent().getSerializableExtra("secret_chat");
        key=getIntent().getStringExtra("key");
        if(secretChat==null){
            finish();
            return;
        }

        String uid= LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_UID);
        dstUid=(uid.equals(secretChat.getSrcUid()))?secretChat.getDstUid():secretChat.getSrcUid();
        UserInfo dstUser= (UserInfo) userInfoHelper.querySingle("SELECT * FROM userinfo WHERE id='"+dstUid+"'");
        setTitle("与 "+dstUser.getName()+" 的绝对安全聊天");

        MessageLoop.addIntent(intentReport);
        MessageLoop.addIntent(intentMessage);
        markRead();
        refresh();
    }

    private void markRead(){
        List<Msg> unread=msgHelper.query("SELECT * FROM msg WHERE srcUid='"+secretChat.getSessionId()+"' AND status="+Msg.STATUS_UNREAD);
        for(Msg msg:unread){
            Map<String,byte[]> param=new HashMap<>();
            param.put("msg_id",msg.getMsgId().getBytes());
            param.put("src_uid",dstUid.getBytes());
            Datagram datagram=new Datagram(Datagram.IDENTIFIER_MARK_READ,param);
            LoopResource.sendDatagram(datagram);
        }
        msgHelper.execSql("UPDATE msg SET status="+Msg.STATUS_READ+" WHERE srcUid='"+secretChat.getSessionId()+"' AND status="+Msg.STATUS_UNREAD);
    }

    public void send(View v){
        String content=et_msg.getText().toString();
        if(TextUtils.isEmpty(content)){
            Toast.makeText(this,"不能为空",Toast.LENGTH_SHORT).show();
            return;
        }

        Msg msg=new Msg(UUIDUtils.getRandomUUID(),LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_UID),secretChat.getSessionId(),content,Msg.MSG_TYPE_SECRET_1,System.currentTimeMillis(),Msg.STATUS_SENDING);
        msg.setContent(AESUtils.aesEncrypt(content,key));

        Map<String,String> sendParam=new HashMap<>();
        sendParam.put("msg", JSON.toJSONString(msg));
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_SEND_MESSAGE,sendParam,"");
        et_msg.setText("");
        LoopResource.sendDatagram(datagram);
        msgHelper.insert(msg);
        refresh();
    }

    private void refresh(){
        List<Msg> msgList=msgHelper.query("SELECT * FROM msg WHERE srcUid='"+secretChat.getSessionId()+"' or dstUid='"+secretChat.getSessionId()+"'");
        lv_msg.setAdapter(new SecretChatAdapter(msgList,this,key));
        lv_msg.setSelection(lv_msg.getCount() - 1);
    }
}

class SecretChatAdapter extends BaseAdapter{

    private List<Msg> msgList;
    private Context context;
    private String uid;
    private String key;

    public SecretChatAdapter(List<Msg> msgList, Context context,String key) {
        this.msgList = msgList;
        this.context = context;
        uid=LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_UID);
        this.key=key;
    }

    @Override
    public int getCount() {
        if(msgList==null)
            return 0;
        return msgList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    private static int textToImage(String text) {
        if (TextUtils.isEmpty(text))
            return -1;
        if (text.equals("手动滑稽"))
            return R.mipmap.sdhj;
        if (text.equals("党员滑稽"))
            return R.mipmap.dyhj;
        if (text.equals("党旗"))
            return R.mipmap.cpc;
        if (text.equals("国旗"))
            return R.mipmap.prc;
        if (text.equals("北社"))
            return R.mipmap.nacp;
        if (text.equals("问号滑稽"))
            return R.mipmap.whhj;
        if(text.equalsIgnoreCase("NASA"))
            return R.mipmap.nasa;
        return -1;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        View v=View.inflate(context,R.layout.view_show_msg,null);
        Msg msg=msgList.get(i);

        try{
            String decrypted=AESUtils.aesDecrypt(msg.getContent(),key);
            msg.setContent(decrypted);
        }catch (Exception e){

        }

        //

        TextView tv_msg=v.findViewById(R.id.tv_msg);
        TextView tv_time=v.findViewById(R.id.tv_time);
        TextView tv_status=v.findViewById(R.id.tv_status);
        ImageView iv=v.findViewById(R.id.iv);
        LinearLayout ll=v.findViewById(R.id.ll);

        if(msg.getSrcUid().equals(uid)){
            //自己发的，东西往右边放
            tv_msg.setGravity(Gravity.RIGHT);
            tv_time.setGravity(Gravity.RIGHT);
            tv_status.setGravity(Gravity.RIGHT);
            ll.setGravity(Gravity.RIGHT);
        }else
            tv_status.setVisibility(View.GONE);

        int image=textToImage(msg.getContent());
        if(image==-1)
            tv_msg.setText(msg.getContent());
        else{
            tv_msg.setVisibility(View.GONE);
            iv.setVisibility(View.VISIBLE);
            iv.setImageResource(image);
        }

        switch (msg.getStatus()){
            case Msg.STATUS_READ:
                tv_status.setText("已读");
                tv_status.setTextColor(Color.GREEN);
                break;
            case Msg.STATUS_UNREAD:
                tv_status.setText("未读");
                tv_status.setTextColor(Color.RED);
                break;
            case Msg.STATUS_SENDING:
                tv_status.setText("正在发送");
                tv_status.setTextColor(Color.BLUE);
                break;
            default:
                tv_status.setText("发送失败");
                tv_status.setTextColor(Color.RED);
                break;
        }

        tv_time.setText(TimeUtils.toStandardTime(msg.getTime()));

        return v;
    }
}
