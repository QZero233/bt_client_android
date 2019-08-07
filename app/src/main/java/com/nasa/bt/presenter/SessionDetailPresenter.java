package com.nasa.bt.presenter;

import android.content.Context;

import com.nasa.bt.SessionDetailActivity;
import com.nasa.bt.contract.SessionDetailContract;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.model.SessionDetailModel;

public class SessionDetailPresenter extends BasePresenter<SessionDetailActivity> implements SessionDetailContract.Presenter {

    private SessionDetailModel model;

    public SessionDetailPresenter(Context context) {
        model=new SessionDetailModel(context);
    }

    @Override
    public void closeSession(String sessionId) {
        if(isViewAttached())
            mView.showProgress();

        SessionDetailContract.Callback callback=new SessionDetailContract.Callback() {
            @Override
            public void onSuccess() {
                if(isViewAttached()){
                    mView.hideProgress();
                    mView.onCloseResult(true);
                }
            }

            @Override
            public void onFailure(int code) {
                if(isViewAttached()){
                    mView.hideProgress();
                    mView.onCloseResult(false);
                }
            }
        };

        model.closeSession(sessionId,callback);
    }

    @Override
    public void updateRemarks(SessionEntity sessionEntity, String newRemarks) {
        if(isViewAttached())
            mView.showProgress();

        SessionDetailContract.Callback callback=new SessionDetailContract.Callback() {
            @Override
            public void onSuccess() {
                if(isViewAttached()){
                    mView.hideProgress();
                    mView.onUpdateResult(true);
                }
            }

            @Override
            public void onFailure(int code) {
                if(isViewAttached()){
                    mView.hideProgress();
                    mView.onUpdateResult(false);
                }
            }
        };

        model.updateRemarks(sessionEntity,newRemarks,callback);
    }

    @Override
    public SessionEntity getSessionEntityBySessionId(String sessionId) {
        return model.getSessionEntityBySessionId(sessionId);
    }

    @Override
    public UserInfoEntity getDstUserInfoBySessionEntity(SessionEntity sessionEntity) {
        return model.getDstUserInfoBySessionEntity(sessionEntity);
    }

    @Override
    public boolean clean(SessionEntity sessionEntity){
        return model.clean(sessionEntity);
    }

}
