package com.nasa.bt;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;

import com.nasa.bt.utils.NotificationUtils;

import org.junit.Test;

public class NotificationTest {

    @Test
    public void sendTest(){
        Context context= InstrumentationRegistry.getTargetContext();

        Intent intent=new Intent(context,MainActivity.class);
        NotificationUtils.sendNotification(context,"2333",intent);
    }

}
