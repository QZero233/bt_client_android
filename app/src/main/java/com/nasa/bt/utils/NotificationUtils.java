package com.nasa.bt.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.nasa.bt.ChatActivity;
import com.nasa.bt.MainActivity;
import com.nasa.bt.R;
import com.nasa.bt.cls.Msg;
import com.nasa.bt.cls.Session;
import com.nasa.bt.cls.UserInfo;

import java.util.List;

public class NotificationUtils {

    public static final String N_ID = "my_channel_01";
    public static final String N_NAME = "name";

    public static void sendNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.mipmap.small_icon_tg);

        CommonDbHelper msgHelper = LocalDbUtils.getMsgHelper(context);
        CommonDbHelper userHelper = LocalDbUtils.getUserInfoHelper(context);
        CommonDbHelper sessionHelper = LocalDbUtils.getSessionHelper(context);

        List<Msg> msgList = msgHelper.query("SELECT * FROM msg WHERE dstUid='" + LocalSettingsUtils.read(context, LocalSettingsUtils.FIELD_UID) + "' and status=" + Msg.STATUS_UNREAD);
        if (msgList == null || msgList.isEmpty())
            return;
        if (msgList.size() == 1) {
            Msg msg = msgList.get(0);

            Session session = (Session) sessionHelper.querySingle("SELECT * FROM session WHERE sessionId='" + msg.getSessionId() + "'");
            if (session == null)
                return;

            UserInfo userInfo = (UserInfo) userHelper.querySingle("SELECT * FROM userinfo WHERE id='" + msg.getSrcUid() + "'");
            if (userInfo == null)
                return;

            if (session.getSessionType() == Session.TYPE_NORMAL) {
                builder.setContentTitle("来自 " + userInfo.getName() + " 的新消息");
                builder.setContentText(msg.getContent());

                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("session", session);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                builder.setContentIntent(pendingIntent);
            } else {
                builder.setContentTitle("来自 " + userInfo.getName() + " 的新加密消息");
                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                builder.setContentIntent(pendingIntent);
            }


        } else {
            boolean one = true;
            int count = 0;
            String lastSessionId = "";
            for (Msg msg : msgList) {
                if (!msg.getSessionId().equals(lastSessionId) && !lastSessionId.equals("")) {
                    one = false;
                    break;
                }
                lastSessionId = msg.getSessionId();
                count++;
            }

            if (one) {
                Session session = (Session) sessionHelper.querySingle("SELECT * FROM session WHERE sessionId='" + lastSessionId + "'");
                if (session == null)
                    return;

                UserInfo userInfo = (UserInfo) userHelper.querySingle("SELECT * FROM userinfo WHERE id='" + lastSessionId + "'");
                if (userInfo == null)
                    return;

                if (session.getSessionType() == Session.TYPE_NORMAL) {
                    builder.setContentTitle("来自 " + userInfo.getName() + " 的" + count + "条新消息");

                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("session", session);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    builder.setContentIntent(pendingIntent);
                } else {
                    builder.setContentTitle("来自 " + userInfo.getName() + " 的" + count + "条新加密消息");
                    Intent intent = new Intent(context, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    builder.setContentIntent(pendingIntent);
                }
            } else {
                builder.setContentTitle("有来自多人的多条未读消息");
                builder.setContentText("多条未读消息");
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
                builder.setContentIntent(pendingIntent);
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(N_ID, N_NAME, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(mChannel);
            builder.setChannelId(N_ID);
        }

        manager.notify(1, builder.build());
    }

}
