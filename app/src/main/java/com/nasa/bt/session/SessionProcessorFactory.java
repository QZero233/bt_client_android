package com.nasa.bt.session;

import com.nasa.bt.data.entity.SessionEntity;

public class SessionProcessorFactory {

    public static SessionProcessor getProcessor(int type){
        if(type== SessionEntity.TYPE_NORMAL)
            return new NormalSessionProcessor();
        if(type==SessionEntity.TYPE_SECRET_CHAT)
            return new SecretSessionProcessor();

        return null;
    }

}
