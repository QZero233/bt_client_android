package com.nasa.bt.session;

import android.content.Context;
import android.content.Intent;

import com.nasa.bt.data.entity.SessionEntity;

public interface SessionProcessor {

    String getName();

    int getSessionTextColor();
    String getChatTitleEndWith();
    String getMainNameEndWith();
    String getMessageMain(SessionEntity sessionEntity);

    void joinSession(SessionEntity sessionEntity, JoinSessionCallback callback, Context context);
    String processContentSent(String content, SessionEntity sessionEntity, Intent intentWithParams);
    String processContentGot(String content, SessionEntity sessionEntity, Intent intentWithParams);

}
