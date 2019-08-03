package com.nasa.bt.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.nasa.bt.ChatActivity;
import com.nasa.bt.R;
import com.nasa.bt.SessionListActivity;
import com.nasa.bt.data.dao.MessageDao;
import com.nasa.bt.data.dao.SessionDao;
import com.nasa.bt.data.dao.UserInfoDao;
import com.nasa.bt.data.entity.MessageEntity;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;

import java.util.List;

/**
 * 部分代码Copy自 https://blog.csdn.net/wjyyawjx/article/details/80020947 如有侵权请告知
 */
public class NotificationUtils extends ContextWrapper {

    private static final Logger log= AppLogConfigurator.getLogger();

    private NotificationManager manager;
    public static final String id = "channel_1";
    public static final String name = "channel_name_1";
    public  Notification notification;

    public NotificationUtils(Context context){
        super(context);
        getManager();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(){
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        getManager().createNotificationChannel(channel);
    }
    private NotificationManager getManager(){
        if (manager == null){
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return manager;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification.Builder getChannelNotification(String title, String content){
        return new Notification.Builder(getApplicationContext(), id)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.bt_icon_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.bt_icon))
                .setAutoCancel(true);
    }

    private Notification.Builder getNotification_25(String title, String content){
        return new Notification.Builder(this).setTicker("123").
                setSmallIcon(R.mipmap.ic_launcher_bt).setLargeIcon( BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_bt))
                .setContentText(content).setContentTitle(title);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void sendNotification(String title, String content,PendingIntent pendingIntent){
        if (Build.VERSION.SDK_INT>=26){
            createNotificationChannel();
            this.notification = getChannelNotification
                    (title, content).setContentIntent(pendingIntent).build();
            getManager().notify(1,notification);
        }else{
            this.notification = getNotification_25(title, content).setContentIntent(pendingIntent).build();
            getManager().notify(1,notification);
        }
    }

    public void cancelNotification(){
        manager.cancelAll();
    }

    public void sendMessageNotification(){
        /**
         * 1.先获取所有未读消息，如无则返回
         * 2.遍历未读消息，如果不是同一会话就显示 有多条未读消息，Intent设置为SessionListActivity（如果本地无该会话就忽略这条消息）
         * 3.如果是同一会话就获取会话信息，如果是普通会话，就显示 有xx发来的n条未读消息，内容设置为最近一条的消息，Intent设置为ChatActivity
         * 4.如果是加密会话，就显示 xx发来的n条加密未读消息，Intent设置为SessionListActivity
         */

        MessageDao messageDao=new MessageDao(this);
        List<MessageEntity> unreadMessages=messageDao.getAllUnreadMessages();
        if(unreadMessages==null || unreadMessages.isEmpty())
            return;

        SessionDao sessionDao=new SessionDao(this);
        String lastSessionId=null;
        boolean same=true;
        for(MessageEntity messageEntity:unreadMessages){
            if(sessionDao.getSessionById(messageEntity.getSessionId())==null)
                continue;

            if(!TextUtils.isEmpty(lastSessionId) && !messageEntity.getSessionId().equals(lastSessionId)){
                same=false;
                break;
            }
            lastSessionId=messageEntity.getSessionId();
        }

        if(!same){
            sendNotification("有多条未读消息","有多条未读消息",PendingIntent.getActivity(this,0,
                    new Intent(this, SessionListActivity.class),PendingIntent.FLAG_UPDATE_CURRENT));

            log.debug("未读消息列表 "+unreadMessages);
        }else{
            SessionEntity sessionEntity=sessionDao.getSessionById(lastSessionId);
            if(sessionEntity==null)
                return;

            UserInfoDao userInfoDao=new UserInfoDao(this);
            UserInfoEntity userInfoEntity=userInfoDao.getUserInfoById(sessionEntity.getIdOfOther(LocalSettingsUtils.read(this,LocalSettingsUtils.FIELD_UID)));
            if(userInfoEntity==null)
                return;

            if(sessionEntity.getSessionType()==SessionEntity.TYPE_NORMAL){
                Intent intent=new Intent(this, ChatActivity.class);
                intent.putExtra("sessionEntity",sessionEntity);
                PendingIntent pendingIntent=PendingIntent.getActivity(this,0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

                sendNotification("有"+unreadMessages.size()+"条来自"+userInfoEntity.getName()+"的未读信息",unreadMessages.get(unreadMessages.size()-1).getContent(),pendingIntent);
            }else if(sessionEntity.getSessionType()==SessionEntity.TYPE_SECRET_CHAT){
                sendNotification("有来自"+userInfoEntity.getName()+"的未读加密信息","有"+unreadMessages.size()+"条来自"+userInfoEntity.getName()+"的加密信息",
                        PendingIntent.getActivity(this,0,
                        new Intent(this, SessionListActivity.class),PendingIntent.FLAG_UPDATE_CURRENT));
            }
        }
    }

    public void sendTestNotification(){
        sendNotification("test","testContent",PendingIntent.getActivity(this,0,
                new Intent(this, SessionListActivity.class),PendingIntent.FLAG_UPDATE_CURRENT));
    }
}
