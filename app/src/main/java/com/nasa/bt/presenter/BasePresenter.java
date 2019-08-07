package com.nasa.bt.presenter;

import com.nasa.bt.contract.BaseView;

public class BasePresenter<V extends BaseView> {

    protected V mView;

    /**
     * 绑定View，写在View初始化的时候
     * @param view
     */
    public void attachView(V view){
        mView=view;
    }

    /**
     * 解绑View，写在View销毁的时候
     */
    public void detachView(){
        mView=null;
    }

    /**
     * 检查View是否被绑定
     * @return 是否被绑定
     */
    protected boolean isViewAttached(){
        return mView!=null;
    }

}
