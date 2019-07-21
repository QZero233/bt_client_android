package com.nasa.bt.loop;

import android.content.Context;

import com.nasa.bt.cls.Datagram;

public interface DatagramListener {
    void onDatagramReach(Datagram datagram);
}
