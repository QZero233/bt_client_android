package com.nasa.bt.presenter;

import android.content.Context;

import com.nasa.bt.contract.SessionListContract;
import com.nasa.bt.model.SessionListModel;
import com.nasa.bt.upgrade.UpgradeStatus;

public class SessionListPresenter extends BasePresenter<SessionListContract.View> implements SessionListContract.Presenter {

    private SessionListModel model;

    public SessionListPresenter(Context context) {
        model=new SessionListModel(context);
    }

    /**
     * 打开主界面之后依次进行
     * 检查更新信息
     * 连接服务器（无论是否已连接都重连，主窗口监听连接情况，如果身份验证失败就跳转&finish）
     * 同步（告诉服务器本地已有的会话ID等，服务器返回客户端没有的）
     * 刷新（服务器返回新消息或新更新）
     */

    @Override
    public void doSync() {
        SessionListContract.Callback callback=new SessionListContract.Callback() {
            @Override
            public void onSuccess() {
                if(isViewAttached()){
                    mView.onSyncSuccess();
                    mView.reloadSessionList(model.getAllSessions());
                    mView.setDrawerHeadInfo(model.getDrawerInfo());
                }
            }

            @Override
            public void onFailure(int code) {
                if(isViewAttached()){
                    mView.onSyncFailure();
                }
            }
        };
        model.doSync(callback);
    }

    @Override
    public void doRefresh() {
        SessionListContract.Callback callback=new SessionListContract.Callback() {

            @Override
            public void onSuccess() {
                if(isViewAttached()){
                    mView.onRefreshSuccess();
                    mView.changeRefreshStatus(false);
                    mView.reloadSessionList(model.getAllSessions());
                }
            }

            @Override
            public void onFailure(int code) {
                if(isViewAttached()){
                    mView.onRefreshFailure();
                    mView.changeRefreshStatus(false);

                }
            }
        };
        model.doRefresh(callback);
    }

    @Override
    public void startListening() {
        SessionListContract.ListenCallback callback=new SessionListContract.ListenCallback() {
            @Override
            public void onDataReach() {
                if(isViewAttached()){
                    mView.reloadSessionList(model.getAllSessions());
                }
            }

            @Override
            public void onAuthFailed() {
                if(isViewAttached()){
                       mView.onAuthFailed();
                }
            }

            @Override
            public void onUpgrade(UpgradeStatus upgradeStatus) {
                if(isViewAttached()){
                    mView.showUpgradeInfo(upgradeStatus);
                }
            }

            @Override
            public void onConnectionStatusChanged(int status) {
                if(isViewAttached()){
                    mView.onConnectionStatusChanged(status);
                }
            }
        };
        model.startListening(callback);
    }

    @Override
    public void stopListening() {
        model.stopListening();
    }

    @Override
    public void reloadSessionList() {
        if(isViewAttached()){
            mView.reloadSessionList(model.getAllSessions());
        }
    }
}
