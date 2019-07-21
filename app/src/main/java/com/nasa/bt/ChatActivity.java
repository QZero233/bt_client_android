package com.nasa.bt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.loop.DatagramListener;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.session.SessionProcessor;
import com.nasa.bt.session.SessionProcessorFactory;
import com.nasa.bt.utils.ImageUtils;
import com.nasa.bt.utils.LocalSettingsUtils;
import com.nasa.bt.utils.TimeUtils;
import com.nasa.bt.utils.UUIDUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private EditText et_msg;
    private ListView lv_msg;

    private MessageDao messageDao;
    private UserInfoDao userInfoDao;
    private SessionDao sessionDao;

    private SessionEntity sessionEntity;
    private SessionProcessor processor;
    private String dstUid, srcUid;



    private DatagramListener changedListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            reload();
            markRead();
        }
    };

    private DatagramListener sessionListener=new DatagramListener() {
        @Override
        public void onDatagramReach(Datagram datagram) {
            reloadSession();
        }
    };

    private long lastClickTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        et_msg = findViewById(R.id.et_msg);
        lv_msg = findViewById(R.id.lv_msg);

        sessionEntity = (SessionEntity) getIntent().getSerializableExtra("sessionEntity");
        processor = SessionProcessorFactory.getProcessor(sessionEntity.getSessionType());

        if (processor == null) {
            finish();
            return;
        }

        srcUid = LocalSettingsUtils.read(this, LocalSettingsUtils.FIELD_UID);
        dstUid = sessionEntity.getIdOfOther(srcUid);

        messageDao = new MessageDao(this);
        userInfoDao = new UserInfoDao(this);
        sessionDao = new SessionDao(this);

        UserInfoEntity dstUser = userInfoDao.getUserInfoById(dstUid);

        if (dstUser == null) {
            finish();
            return;
        }

        reload();
        registerListener();
        markRead();

        setTitle("与 " + dstUser.getName() + " 的" + processor.getSessionProperties().getChatTitleEndWith());
        reloadSession();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (sessionEntity.isDisabled()) {
            getMenuInflater().inflate(R.menu.menu_chat_disabled, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_chat, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.m_clean) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("确认清除聊天记录");
            builder.setMessage("操作不可逆");
            builder.setNegativeButton("取消", null);
            builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (messageDao.deleteAllMessage(sessionEntity.getSessionId())) {
                        reload();
                        Toast.makeText(ChatActivity.this, "操作成功", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(ChatActivity.this, "操作失败", Toast.LENGTH_SHORT).show();

                }
            });
            builder.show();
        } else if (item.getItemId() == R.id.m_image) {
            showImageDialog();
        } else if (item.getItemId() == R.id.m_detail) {
            Intent intent = new Intent(this, SessionDetailActivity.class);
            intent.putExtra("sessionEntity", sessionEntity);
            startActivity(intent);
            finish();
        } else if (item.getItemId() == R.id.m_delete) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("确认删除会话并清空聊天记录");
            builder.setMessage("操作不可逆");
            builder.setNegativeButton("取消", null);
            builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (messageDao.deleteAllMessage(sessionEntity.getSessionId()) && sessionDao.deleteSession(sessionEntity.getSessionId())) {
                        Toast.makeText(ChatActivity.this, "操作成功", Toast.LENGTH_SHORT).show();
                        finish();
                    } else
                        Toast.makeText(ChatActivity.this, "操作失败", Toast.LENGTH_SHORT).show();

                }
            });
            builder.show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImageDialog(){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);

        View v=View.inflate(this,R.layout.view_show_images,null);
        final Spinner spinnerSeries=v.findViewById(R.id.sp_series);
        final Spinner spinnerDetail=v.findViewById(R.id.sp_detail);
        final ImageView iv=v.findViewById(R.id.iv);

        iv.setMinimumWidth(100);
        iv.setMinimumHeight(100);

        Object[] keys= ImageUtils.images.keySet().toArray();
        ArrayAdapter arrayAdapter=new ArrayAdapter(this,R.layout.item_image_sp,keys);
        spinnerSeries.setAdapter(arrayAdapter);

        spinnerSeries.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                List<String> detail=ImageUtils.images.get(spinnerSeries.getSelectedItem().toString());
                if(detail==null)
                    return;

                ArrayAdapter arrayAdapter=new ArrayAdapter(ChatActivity.this,R.layout.item_image_sp,detail);
                spinnerDetail.setAdapter(arrayAdapter);
                spinnerDetail.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinnerDetail.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int image=ImageUtils.textToImage(spinnerDetail.getSelectedItem().toString());
                if(image!=-1)
                    iv.setImageResource(image);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        builder.setView(v);
        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("选择", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                et_msg.setText(spinnerDetail.getSelectedItem().toString());
            }
        });

        builder.show();
    }

    private void registerListener(){
        MessageLoopUtils.registerListenerNormal("CHAT_REPORT",Datagram.IDENTIFIER_REPORT,changedListener);
        MessageLoopUtils.registerListenerNormal("CHAR_MESSAGE_DETAIL",Datagram.IDENTIFIER_MESSAGE_DETAIL,changedListener);
        MessageLoopUtils.registerListenerNormal("CHAR_SESSION_UPDATE",Datagram.IDENTIFIER_UPDATE_DETAIL,sessionListener);
    }

    private void unregisterListener(){
        MessageLoopUtils.unregisterListener("CHAT_REPORT");
        MessageLoopUtils.unregisterListener("CHAR_MESSAGE_DETAIL");
        MessageLoopUtils.unregisterListener("CHAR_SESSION_UPDATE");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterListener();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        registerListener();
        markRead();
    }

    private void markRead() {
        List<MessageEntity> unread = messageDao.getUnreadMessageBySessionId(sessionEntity.getSessionId());
        for (MessageEntity messageEntity : unread) {
            Map<String, byte[]> param = new HashMap<>();
            param.put("msg_id", messageEntity.getMsgId().getBytes());
            param.put("src_uid", dstUid.getBytes());
            Datagram datagram = new Datagram(Datagram.IDENTIFIER_MARK_READ, param);
            SendDatagramUtils.sendDatagram(datagram);
        }
    }

    private void reload() {
        List<MessageEntity> messageEntities = messageDao.getAllMessage(sessionEntity.getSessionId());
        lv_msg.setAdapter(new ChatMsgAdapter(messageEntities, this, dstUid, getIntent(), sessionEntity));
        lv_msg.setSelection(lv_msg.getCount() - 1);
        lv_msg.setOnItemClickListener(this);
    }

    private void reloadSession(){
        sessionEntity=sessionDao.getSessionById(sessionEntity.getSessionId());
        if(sessionEntity==null){
            Toast.makeText(this,"不知为何找不到此会话了（手动滑稽）",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if(sessionEntity.isDisabled()){
            Button btnSend=findViewById(R.id.btn_send);
            btnSend.setVisibility(View.GONE);
            et_msg.setVisibility(View.GONE);
            setTitle(getTitle()+"（会话已关闭）");
        }
    }

    public void send(View v) {
        String content = et_msg.getText().toString();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        content = processor.processContentSent(content, sessionEntity, getIntent());

        MessageEntity messageEntity = new MessageEntity(UUIDUtils.getRandomUUID(), srcUid, dstUid, sessionEntity.getSessionId(), content, System.currentTimeMillis(), MessageEntity.STATUS_SENDING);


        Datagram datagram = new Datagram(Datagram.IDENTIFIER_SEND_MESSAGE, new ParamBuilder().putParam("msg", JSON.toJSONString(messageEntity)).build());
        et_msg.setText("");
        SendDatagramUtils.sendDatagram(datagram);
        messageDao.addMessage(messageEntity);
        updateSessionInfo(messageEntity);
        reload();
    }

    private void updateSessionInfo(MessageEntity messageEntity) {
        if (sessionEntity.getSessionType() == SessionEntity.TYPE_SECRET_CHAT)
            messageEntity.setContent("加密信息，需密码解密查看");

        sessionDao.changeLastStatus(sessionEntity.getSessionId(), messageEntity.getContent(), messageEntity.getTime());
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        long currentClickTime = System.currentTimeMillis();
        long deltaT = currentClickTime - lastClickTime;
        if (deltaT > 0 && deltaT < 1000) {
            //双击事件
            startActivity(new Intent(this, SDGameActivity.class));
            finish();
        }

        lastClickTime = currentClickTime;
    }
}

class ChatMsgAdapter extends BaseAdapter {

    private List<MessageEntity> messageEntities;
    private Context context;
    private String dstUid;
    private Intent intent;
    private SessionProcessor processor;
    private SessionEntity sessionEntity;

    public ChatMsgAdapter(List<MessageEntity> messageEntities, Context context, String dstUid, Intent intent, SessionEntity sessionEntity) {
        this.messageEntities = messageEntities;
        this.context = context;
        this.dstUid = dstUid;
        this.intent = intent;
        this.sessionEntity = sessionEntity;
        processor = SessionProcessorFactory.getProcessor(sessionEntity.getSessionType());
    }

    @Override
    public int getCount() {
        if (messageEntities == null || messageEntities.isEmpty())
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

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        View v = View.inflate(context, R.layout.view_show_msg, null);
        MessageEntity messageEntity = messageEntities.get(i);
        String content = messageEntity.getContent();

        try {
            content = processor.processContentGot(content, sessionEntity, intent);
        } catch (Exception e) {

        }


        TextView tv_msg = v.findViewById(R.id.tv_msg);
        TextView tv_time = v.findViewById(R.id.tv_time);
        TextView tv_status = v.findViewById(R.id.tv_status);
        ImageView iv = v.findViewById(R.id.iv);
        LinearLayout ll = v.findViewById(R.id.ll);

        if (!messageEntity.getSrcUid().equals(dstUid)) {
            //自己发的，东西往右边放
            tv_msg.setGravity(Gravity.RIGHT);
            tv_time.setGravity(Gravity.RIGHT);
            tv_status.setGravity(Gravity.RIGHT);
            ll.setGravity(Gravity.RIGHT);
        } else
            tv_status.setVisibility(View.GONE);

        int image = ImageUtils.textToImage(content);
        if (image == -1)
            tv_msg.setText(content);
        else {
            tv_msg.setVisibility(View.GONE);
            iv.setVisibility(View.VISIBLE);
            iv.setImageResource(image);

            iv.setMinimumWidth(100);
            iv.setMinimumHeight(100);
        }

        switch (messageEntity.getStatus()) {
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
