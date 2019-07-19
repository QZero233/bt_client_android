package com.nasa.bt.session;

import android.content.Context;
import android.content.Intent;

import com.nasa.bt.data.entity.SessionEntity;

public interface SessionProcessor {

    /**
     * 获取该类会话的SessionProperties对象
     * 对象封装了会话名称等会话基本信息
     */
    SessionProperties getSessionProperties();

    /**
     * 获取在SessionListActivity里面ListView中应显示的消息文字内容
     */
    String getMessageMain(SessionEntity sessionEntity);

    /**
     * 加入该会话
     * 加入前会进行一些信息采集，如果通过就会调用callback的start方法并传入采集到的信息
     */
    void joinSession(SessionEntity sessionEntity, JoinSessionCallback callback, Context context);

    /**
     * 发出消息前对消息进行处理
     * @param content
     * @param sessionEntity
     * @param intentWithParams
     * @return
     */
    String processContentSent(String content, SessionEntity sessionEntity, Intent intentWithParams);

    /**
     * 显示消息前对消息进行处理
     * @param content
     * @param sessionEntity
     * @param intentWithParams
     * @return
     */
    String processContentGot(String content, SessionEntity sessionEntity, Intent intentWithParams);

}
