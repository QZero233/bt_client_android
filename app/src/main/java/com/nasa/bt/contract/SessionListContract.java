package com.nasa.bt.contract;

import com.nasa.bt.data.entity.SessionEntityForShow;
import com.nasa.bt.upgrade.UpgradeStatus;

import java.util.List;

public class SessionListContract {

    public interface Model{
        void doSync(Callback callback);
        void doRefresh(Callback callback);
        List<SessionEntityForShow> getAllSessions();
        String getDrawerInfo();
        void startListening(ListenCallback callback);
        void stopListening();
    }

    public interface Presenter{
        void doSync();
        void doRefresh();

        void startListening();
        void stopListening();

        void reloadSessionList();
    }

    public interface View extends BaseView{
        void changeRefreshStatus(boolean isRefreshing);
        void reloadSessionList(List<SessionEntityForShow> sessionEntityList);
        void setDrawerHeadInfo(String name);
        void showUpgradeInfo(UpgradeStatus upgradeStatus);

        void onConnectionStatusChanged(int status);

        void onSyncFailure();
        void onSyncSuccess();

        void onRefreshFailure();
        void onRefreshSuccess();

        void onAuthFailed();
    }

    public interface Callback{
        void onSuccess();
        void onFailure(int code);
    }

    public interface ListenCallback{
        void onDataReach();
        void onAuthFailed();
        void onUpgrade(UpgradeStatus upgradeStatus);
        void onConnectionStatusChanged(int status);
    }

}
