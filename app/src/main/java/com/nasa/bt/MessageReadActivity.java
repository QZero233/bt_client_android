package com.nasa.bt;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MessageReadActivity extends AppCompatActivity {

    private String msg;
    private TextView tv_msg;
    private TextInputLayout til_msg;
    private TextInputEditText et_msg;

    private boolean readMode=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_read);

        msg=getIntent().getStringExtra("message");
        if(msg==null)
            msg="";

        tv_msg=findViewById(R.id.tv_msg);
        til_msg=findViewById(R.id.til_msg);
        et_msg=findViewById(R.id.et_msg);

        tv_msg.setText(msg);
        tv_msg.setMovementMethod(ScrollingMovementMethod.getInstance());
        tv_msg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                copy();
                return true;
            }
        });
    }

    public void copy(){
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData mClipData = ClipData.newPlainText("Result", msg);
        cm.setPrimaryClip(mClipData);
        Toast.makeText(this,"复制成功",Toast.LENGTH_SHORT).show();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_read,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.m_read_read){
            if(readMode)
                return super.onOptionsItemSelected(item);
            readMode=true;
            msg=et_msg.getText().toString();
            til_msg.setVisibility(View.INVISIBLE);
            tv_msg.setText(msg);
            tv_msg.setVisibility(View.VISIBLE);
            Toast.makeText(this,"阅读模式，长按复制全文",Toast.LENGTH_SHORT).show();
        }

        if(item.getItemId()==R.id.m_read_edit){
            if(!readMode)
                return super.onOptionsItemSelected(item);
            readMode=false;
            til_msg.setVisibility(View.VISIBLE);
            tv_msg.setVisibility(View.INVISIBLE);
            et_msg.setText(msg);
            Toast.makeText(this,"编辑模式，编辑完成后点击阅读保存",Toast.LENGTH_SHORT).show();
        }


        return super.onOptionsItemSelected(item);
    }

}
