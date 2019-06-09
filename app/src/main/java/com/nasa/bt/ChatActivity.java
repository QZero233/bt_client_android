package com.nasa.bt;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.UserInfo;
import com.nasa.bt.crypt.CryptModule;
import com.nasa.bt.crypt.CryptModuleFactory;
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

public class ChatActivity extends AppCompatActivity {

    private TextInputEditText et_msg;
    private ListView lv_msg;
    private CommonDbHelper msgHelper;
    private String uidDst;
    private UserInfo userDst;

    private CryptModule chatModule;

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

        chatModule= CryptModuleFactory.getCryptModule(CryptModuleFactory.CURRENT_CRYPT_MODULE_CHAT);

        userDst= (UserInfo) getIntent().getSerializableExtra("userDst");
        if(userDst==null){
            finish();
            return;
        }
        uidDst=userDst.getId();
        msgHelper= LocalDbUtils.getMsgHelper(this);
        reload();

        MessageLoop.addIntent(intentReport);
        MessageLoop.addIntent(intentMessage);
        markRead();

        setTitle("与 "+userDst.getName()+" 的加密通信");
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
                    msgHelper.execSql("DELETE FROM msg WHERE dstUid='"+uidDst+"' OR srcUid='"+uidDst+"'");
                    reload();
                    Toast.makeText(ChatActivity.this,"操作成功",Toast.LENGTH_SHORT).show();
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

    private void markRead(){
        List<Msg> unread=msgHelper.query("SELECT * FROM msg WHERE srcUid='"+uidDst+"' AND status="+Msg.STATUS_UNREAD);
        for(Msg msg:unread){
            Map<String,byte[]> param=new HashMap<>();
            param.put("msg_id",msg.getMsgId().getBytes());
            param.put("src_uid",uidDst.getBytes());
            Datagram datagram=new Datagram(Datagram.IDENTIFIER_MARK_READ,param);
            LoopResource.sendDatagram(datagram);
        }
        msgHelper.execSql("UPDATE msg SET status="+Msg.STATUS_READ+" WHERE srcUid='"+uidDst+"' AND status="+Msg.STATUS_UNREAD);
    }

    private void reload(){
        List<Msg> msgs=msgHelper.query("SELECT * FROM msg WHERE srcUid='"+uidDst+"' or dstUid='"+uidDst+"' ORDER BY time");
        lv_msg.setAdapter(new ChatMsgAdapter(msgs,this,uidDst));
        lv_msg.setSelection(lv_msg.getCount() - 1);
    }

    public void send(View v){
        String content=et_msg.getText().toString();
        if(TextUtils.isEmpty(content)){
            Toast.makeText(this,"不能为空",Toast.LENGTH_SHORT).show();
            return;
        }

        Msg msgInsert=new Msg(UUIDUtils.getRandomUUID(),"",uidDst,content,System.currentTimeMillis(),Msg.STATUS_SENDING);

        if(chatModule!=null){
            byte[] encrypted=chatModule.doEncrypt(content.getBytes(),userDst.getKey(),null);
            if(encrypted!=null)
                content=new String(encrypted);
        }

        Msg msg=new Msg(msgInsert.getMsgId(),"",uidDst,content,System.currentTimeMillis(),Msg.STATUS_SENDING);


        Map<String,byte[]> sendParam=new HashMap<>();
        sendParam.put("msg_id", msg.getMsgId().getBytes());
        sendParam.put("dst_uid",uidDst.getBytes());
        sendParam.put("msg_content",content.getBytes());
        Datagram datagram=new Datagram(Datagram.IDENTIFIER_SEND_MESSAGE,sendParam);
        et_msg.setText("");
        LoopResource.sendDatagram(datagram);

        msgHelper.insert(msgInsert);
        reload();
    }
}

class ChatMsgAdapter extends BaseAdapter{

    private List<Msg> msgs;
    private Context context;
    private String dstUid;

    public ChatMsgAdapter(List<Msg> msgs, Context context,String dstUid) {
        this.msgs = msgs;
        this.context = context;
        this.dstUid=dstUid;
    }

    @Override
    public int getCount() {
        if(msgs==null || msgs.isEmpty())
            return 0;
        return msgs.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        View v=View.inflate(context,R.layout.view_show_msg,null);
        Msg msg=msgs.get(i);

        TextView tv_msg=v.findViewById(R.id.tv_msg);
        TextView tv_time=v.findViewById(R.id.tv_time);
        TextView tv_status=v.findViewById(R.id.tv_status);

        if(!msg.getSrcUid().equals(dstUid)){
            //自己发的，东西往右边放
            tv_msg.setGravity(Gravity.RIGHT);
            tv_time.setGravity(Gravity.RIGHT);
            tv_status.setGravity(Gravity.RIGHT);
        }else
            tv_status.setVisibility(View.GONE);

        tv_msg.setText(msg.getContent());

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
