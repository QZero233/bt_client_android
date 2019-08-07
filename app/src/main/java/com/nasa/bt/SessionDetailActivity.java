package com.nasa.bt;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nasa.bt.contract.SessionDetailContract;
import com.nasa.bt.data.entity.SessionEntity;
import com.nasa.bt.data.entity.UserInfoEntity;
import com.nasa.bt.presenter.SessionDetailPresenter;
import com.nasa.bt.session.JoinSessionCallback;
import com.nasa.bt.session.SessionProcessor;
import com.nasa.bt.session.SessionProcessorFactory;

public class SessionDetailActivity extends AppCompatActivity implements SessionDetailContract.View {

    private SessionEntity sessionEntity;
    private SessionProcessor processor;

    private TextView tv_name,tv_type,tv_remarks;
    private ProgressBar pb;

    private SessionDetailPresenter presenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        sessionEntity= (SessionEntity) getIntent().getSerializableExtra("sessionEntity");
        if(sessionEntity==null){
            finish();
            return;
        }

        presenter=new SessionDetailPresenter(this);
        presenter.attachView(this);

        tv_name=findViewById(R.id.tv_name);
        tv_type=findViewById(R.id.tv_type);
        tv_remarks=findViewById(R.id.tv_remarks);
        pb=findViewById(R.id.pb);

        processor= SessionProcessorFactory.getProcessor(sessionEntity.getSessionType());


        UserInfoEntity userInfoEntity=presenter.getDstUserInfoBySessionEntity(sessionEntity);

        tv_name.setText(userInfoEntity.getName());
        tv_type.setText(processor.getSessionProperties().getSessionName());

        String remarks=sessionEntity.getSpecifiedParam("remarks");
        if(remarks==null)
            remarks="";
        tv_remarks.setText(remarks);


        if(!sessionEntity.isDisabled()){
            Button btn_clean=findViewById(R.id.btn_clean);
            btn_clean.setText("清空聊天记录");
        }else{
            Button btn_update=findViewById(R.id.btn_update);
            btn_update.setVisibility(View.GONE);
            Button btn_close=findViewById(R.id.btn_close);
            btn_close.setVisibility(View.GONE);
        }
    }

    public void close(View v){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("是否关闭");
        builder.setMessage("操作不可逆，会保留本地聊天记录");
        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("关闭", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                presenter.closeSession(sessionEntity.getSessionId());
            }
        });
        builder.show();
    }

    public void clean(View v){
        String message;
        if(sessionEntity.isDisabled()){
            message="确认删除会话并清空聊天记录";
        }else{
            message="确认清除聊天记录";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(message);
        builder.setMessage("操作不可逆");
        builder.setNegativeButton("取消", null);
        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (presenter.clean(sessionEntity)) {
                    Toast.makeText(SessionDetailActivity.this, "操作成功", Toast.LENGTH_SHORT).show();

                    if(sessionEntity.isDisabled())
                        finish();
                } else
                    Toast.makeText(SessionDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show();

            }
        });
        builder.show();
    }

    public void updateDetail(View view){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);

        final EditText et_remarks=new EditText(this);
        String remarks=sessionEntity.getSpecifiedParam("remarks");
        if(remarks==null)
            remarks="";
        et_remarks.setText(remarks);

        builder.setTitle("请输入新备注");
        builder.setView(et_remarks);
        builder.setCancelable(false);

        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("更改", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String remarks=et_remarks.getText().toString();
                presenter.updateRemarks(sessionEntity,remarks);
                Toast.makeText(SessionDetailActivity.this,"正在更改......",Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.detachView();
    }

    public void join(View v){
        if(processor==null)
            return;

        JoinSessionCallback callback=new JoinSessionCallback() {
            @Override
            public void start(Intent intentWithParams) {
                if(intentWithParams ==null)
                    intentWithParams=new Intent();
                intentWithParams.putExtra("sessionEntity",sessionEntity);
                intentWithParams.setClass(SessionDetailActivity.this,ChatActivity.class);
                startActivity(intentWithParams);
                finish();
            }
        };

        processor.joinSession(sessionEntity,callback,this);
    }

    @Override
    public void showProgress() {
        pb.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        pb.setVisibility(View.GONE);
    }

    @Override
    public void onCloseResult(boolean isSucceed) {
        if(isSucceed){
            Toast.makeText(SessionDetailActivity.this, "关闭成功", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(SessionDetailActivity.this, "关闭失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpdateResult(boolean isSucceed) {
        if(isSucceed){
            Toast.makeText(SessionDetailActivity.this, "更改成功", Toast.LENGTH_SHORT).show();
            tv_remarks.setText(sessionEntity.getSpecifiedParam("remarks"));
        } else {
            Toast.makeText(SessionDetailActivity.this, "更改失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showToast(String content) {
        Toast.makeText(this,content,Toast.LENGTH_SHORT).show();
    }
}
