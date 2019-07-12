package com.nasa.bt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
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
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.crypt.AESUtils;
import com.nasa.bt.loop.LoopResource;
import com.nasa.bt.loop.MessageIntent;
import com.nasa.bt.loop.MessageLoop;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.TimeUtils;
import com.nasa.bt.utils.UUIDUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private EditText et_msg;
    private ListView lv_msg;

    private MessageDao messageDao;
    private UserInfoDao userInfoDao;
    private SessionDao sessionDao;

    private SessionEntity sessionEntity;
    private String dstUid,srcUid;

    private byte[] aesKey=null;

    private Handler changedHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            reload();
            markRead();
        }
    };

    private MessageIntent intentReport=new MessageIntent(UUIDUtils.getRandomUUID(), Datagram.IDENTIFIER_REPORT,changedHandler,0,1);
    private MessageIntent intentMessage=new MessageIntent(UUIDUtils.getRandomUUID(), Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,changedHandler,0,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        et_msg=findViewById(R.id.et_msg);
        lv_msg=findViewById(R.id.lv_msg);

        sessionEntity = (SessionEntity) getIntent().getSerializableExtra("sessionEntity");
        srcUid=LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_UID);
        dstUid= sessionEntity.getIdOfOther(srcUid);

        messageDao=new MessageDao(this);
        userInfoDao=new UserInfoDao(this);
        sessionDao=new SessionDao(this);

        UserInfoEntity dstUser=userInfoDao.getUserInfoById(dstUid);

        if(dstUser==null){
            finish();
            return;
        }

        reload();

        MessageLoop.addIntent(intentReport);
        MessageLoop.addIntent(intentMessage);
        markRead();

        setTitle("与 "+dstUser.getName()+" 的安全通信");
        if(sessionEntity.getSessionType()== SessionEntity.TYPE_SECRET_CHAT){
            setTitle("与 "+dstUser.getName()+" 的绝对安全通信");
            aesKey=AESUtils.getAESKey(getIntent().getStringExtra("key"));
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.m_clean){
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setMessage("确认删除，操作不可逆？");
            builder.setNegativeButton("取消",null);
            builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(messageDao.deleteAllMessage(dstUid)){
                        reload();
                        Toast.makeText(ChatActivity.this,"操作成功",Toast.LENGTH_SHORT).show();
                    }else
                        Toast.makeText(ChatActivity.this,"操作失败",Toast.LENGTH_SHORT).show();

                }
            });
            builder.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageLoop.removeIntent(Datagram.IDENTIFIER_REPORT,intentReport.getId(),1);
        MessageLoop.removeIntent(Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,intentMessage.getId(),1);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MessageLoop.removeIntent(Datagram.IDENTIFIER_REPORT,intentReport.getId(),1);
        MessageLoop.removeIntent(Datagram.IDENTIFIER_RETURN_MESSAGE_DETAIL,intentMessage.getId(),1);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        MessageLoop.addIntent(intentReport);
        MessageLoop.addIntent(intentMessage);
        markRead();
    }

    private void markRead(){
        List<MessageEntity> unread=messageDao.getUnreadMessageBySessionId(sessionEntity.getSessionId());
        for(MessageEntity messageEntity :unread){
            Map<String,byte[]> param=new HashMap<>();
            param.put("msg_id", messageEntity.getMsgId().getBytes());
            param.put("src_uid",dstUid.getBytes());
            Datagram datagram=new Datagram(Datagram.IDENTIFIER_MARK_READ,param);
            LoopResource.sendDatagram(datagram);
        }
    }

    private void reload(){
        List<MessageEntity> messageEntities=messageDao.getAllMessage(sessionEntity.getSessionId());
        lv_msg.setAdapter(new ChatMsgAdapter(messageEntities,this,dstUid,getIntent(), sessionEntity.getSessionType()));
        lv_msg.setSelection(lv_msg.getCount() - 1);
    }

    public void send(View v){
        String content=et_msg.getText().toString();
        if(TextUtils.isEmpty(content)){
            Toast.makeText(this,"不能为空",Toast.LENGTH_SHORT).show();
            return;
        }

        if(sessionEntity.getSessionType()== SessionEntity.TYPE_SECRET_CHAT){
            content=AESUtils.aesEncrypt(content,aesKey);
        }

        MessageEntity messageEntity =new MessageEntity(UUIDUtils.getRandomUUID(),srcUid,dstUid, sessionEntity.getSessionId(),content,System.currentTimeMillis(), MessageEntity.STATUS_SENDING);

        Map<String,String> sendParam=new HashMap<>();
        sendParam.put("msg", JSON.toJSONString(messageEntity));
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_SEND_MESSAGE,sendParam,"");
        et_msg.setText("");
        LoopResource.sendDatagram(datagram);
        messageDao.addMessage(messageEntity);
        updateSessionInfo(messageEntity);
        reload();
    }

    private void updateSessionInfo(MessageEntity messageEntity){
        if(sessionEntity.getSessionType()== SessionEntity.TYPE_SECRET_CHAT)
            messageEntity.setContent("加密信息，需密码解密查看");

        sessionDao.changeLastStatus(sessionEntity.getSessionId(),messageEntity.getContent(),messageEntity.getTime());
    }
}

class ChatMsgAdapter extends BaseAdapter{

    private List<MessageEntity> messageEntities;
    private Context context;
    private String dstUid;
    private Intent intent;
    private int sessionType;

    private byte[] aesKey=null;

    public ChatMsgAdapter(List<MessageEntity> messageEntities, Context context, String dstUid, Intent intent, int sessionType) {
        this.messageEntities = messageEntities;
        this.context = context;
        this.dstUid=dstUid;
        this.intent=intent;
        this.sessionType=sessionType;
    }

    @Override
    public int getCount() {
        if(messageEntities ==null || messageEntities.isEmpty())
            return 0;
        return messageEntities.size();
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
        MessageEntity messageEntity = messageEntities.get(i);
        String content= messageEntity.getContent();

        try {
            if(sessionType== SessionEntity.TYPE_SECRET_CHAT){
                if(aesKey==null)
                    aesKey=AESUtils.getAESKey(intent.getStringExtra("key"));
                content=AESUtils.aesDecrypt(messageEntity.getContent(),aesKey);
            }
        }catch (Exception e){

        }


        TextView tv_msg=v.findViewById(R.id.tv_msg);
        TextView tv_time=v.findViewById(R.id.tv_time);
        TextView tv_status=v.findViewById(R.id.tv_status);
        ImageView iv=v.findViewById(R.id.iv);
        LinearLayout ll=v.findViewById(R.id.ll);

        if(!messageEntity.getSrcUid().equals(dstUid)){
            //自己发的，东西往右边放
            tv_msg.setGravity(Gravity.RIGHT);
            tv_time.setGravity(Gravity.RIGHT);
            tv_status.setGravity(Gravity.RIGHT);
            ll.setGravity(Gravity.RIGHT);
        }else
            tv_status.setVisibility(View.GONE);

        int image=textToImage(content);
        if(image==-1)
            tv_msg.setText(content);
        else{
            tv_msg.setVisibility(View.GONE);
            iv.setVisibility(View.VISIBLE);
            iv.setImageResource(image);
        }

        switch (messageEntity.getStatus()){
            case MessageEntity.STATUS_READ:
                tv_status.setText("已读");
                tv_status.setTextColor(Color.GREEN);
                break;
            case MessageEntity.STATUS_UNREAD:
                tv_status.setText("未读");
                tv_status.setTextColor(Color.RED);
                break;
            case MessageEntity.STATUS_SENDING:
                tv_status.setText("正在发送");
                tv_status.setTextColor(Color.BLUE);
                break;
            default:
                tv_status.setText("发送失败");
                tv_status.setTextColor(Color.RED);
                break;
        }

        tv_time.setText(TimeUtils.toStandardTime(messageEntity.getTime()));


        return v;
    }
}
