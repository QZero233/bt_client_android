package com.nasa.bt.contract;

import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;

public class SessionDetailContract {

    public interface Model{
        void closeSession(String sessionId,Callback callback);
        void updateRemarks(SessionEntity sessionEntity,String newRemarks,Callback callback);

        SessionEntity getSessionEntityBySessionId(String sessionId);
        UserInfoEntity getDstUserInfoBySessionEntity(SessionEntity sessionEntity);

        boolean clean(SessionEntity sessionEntity);
    }

    public interface Presenter{
        void closeSession(String sessionId);
        void updateRemarks(SessionEntity sessionEntity,String newRemarks);

        SessionEntity getSessionEntityBySessionId(String sessionId);
        UserInfoEntity getDstUserInfoBySessionEntity(SessionEntity sessionEntity);

        boolean clean(SessionEntity sessionEntity);
    }

    public interface View extends BaseView{
        void showProgress();
        void hideProgress();

        void onCloseResult(boolean isSucceed);
        void onUpdateResult(boolean isSucceed);
    }

    public interface Callback{
        void onSuccess();
        void onFailure(int code);
    }

}
