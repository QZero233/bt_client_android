package com.nasa.bt.session;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.nasa.bt.data.entity.SessionEntity;

public class NormalSessionProcessor implements SessionProcessor {

    @Override
    public SessionProperties getSessionProperties() {
        return new SessionProperties("普通会话",-1,"安全通信","");
    }

    @Override
    public String getMessageMain(SessionEntity sessionEntity) {
        return sessionEntity.getLastMessage();
    }

    @Override
    public void joinSession(SessionEntity sessionEntity, JoinSessionCallback callback, Context context) {
        callback.start(null);
    }

    @Override
    public String processContentSent(String content, SessionEntity sessionEntity, Intent intentWithParams) {
        return content;
    }

    @Override
    public String processContentGot(String content, SessionEntity sessionEntity, Intent intentWithParams) {
        return content;
    }
}
