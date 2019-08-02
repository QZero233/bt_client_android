package com.nasa.bt.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Random;

public class TrustedKeyFragment extends Fragment implements Runnable {

    private TextView tv;

    private Handler updateHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            int size=msg.arg1;
            int color=msg.arg2;
            tv.setTextSize(size);
            tv.setTextColor(color);
        }
    };

    private static final int[] colors={Color.RED,Color.BLACK,Color.BLUE,Color.GREEN,Color.GRAY,Color.CYAN};

    private boolean running=true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        tv=new TextView(getContext());
        tv.setText("然而这个功能作者并没完成");
        tv.setTextColor(Color.BLUE);
        tv.setTextSize(20);

        new Thread(this).start();

        return tv;
    }

    @Override
    public void run() {
        while (running){
            try {
                Thread.sleep(500);
            }catch (Exception e){

            }

            Random random=new Random();
            int size=(Math.abs(random.nextInt())%50)+20;
            int colorIndex=Math.abs(random.nextInt())%colors.length;

            Message msg=new Message();
            msg.arg1=size;
            msg.arg2=colors[colorIndex];
            updateHandler.sendMessage(msg);
        }
    }
}
