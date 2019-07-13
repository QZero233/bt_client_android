package com.nasa.bt.session;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.nasa.bt.data.entity.SessionEntity;

public class NormalSessionProcessor implements SessionProcessor {
    @Override
    public String getName() {
        return "普通会话";
    }

    @Override
    public int getSessionTextColor() {
        return -1;
    }

    @Override
    public String getChatTitleEndWith() {
        return "安全通信";
    }

    @Override
    public String getMainNameEndWith() {
        return "";
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
