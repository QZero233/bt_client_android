package com.nasa.bt;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.crypt.AppKeyStore;
import com.nasa.bt.crypt.RSAUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RSAKeyManagerActivity extends AppCompatActivity implements View.OnLongClickListener, AdapterView.OnItemSelectedListener {

    private TextView tv_pub,tv_pri;
    private Spinner sp_keys;

    private AppKeyStore appKeyStore;
    private RSAKeySet currentKeySet;

    private List<RSAKeySet> keySets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rsakey_manager);

        tv_pub=findViewById(R.id.tv_pub);
        tv_pri=findViewById(R.id.tv_pri);
        sp_keys=findViewById(R.id.sp_keys);

        tv_pri.setMovementMethod(ScrollingMovementMethod.getInstance());
        appKeyStore=AppKeyStore.getInstance();

        tv_pub.setOnLongClickListener(this);
        tv_pri.setOnLongClickListener(this);

        reload();

        sp_keys.setSelection(LocalSettingsUtils.readInt(this,LocalSettingsUtils.FIELD_CURRENT_KEY_SET_INDEX));
        sp_keys.setOnItemSelectedListener(this);

        verifyStoragePermissions();
    }

    private void reload(){
        currentKeySet=appKeyStore.getCurrentKeySet();
        keySets=appKeyStore.getKeySets();

        List<String> keys=new ArrayList<>();
        for(RSAKeySet keySet:keySets){
            keys.add(keySet.getName());
        }

        ArrayAdapter arrayAdapter=new ArrayAdapter(this,R.layout.item_image_sp,keys);
        sp_keys.setAdapter(arrayAdapter);
        setKeysTextView();
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()){
            case R.id.tv_pub:
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("PublicKey", currentKeySet.getPub());
                cm.setPrimaryClip(mClipData);
                Toast.makeText(this, "复制成功", Toast.LENGTH_SHORT).show();
                break;
            case R.id.tv_pri:
                ClipboardManager cm2 = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData mClipData2 = ClipData.newPlainText("PrivateKey", currentKeySet.getPri());
                cm2.setPrimaryClip(mClipData2);
                Toast.makeText(this, "复制成功", Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    private void setKeysTextView(){
        tv_pub.setText("公钥（长按复制）\n"+currentKeySet.getPub());
        tv_pri.setText("私钥（长按复制）\n"+currentKeySet.getPri());
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        appKeyStore.switchKeySet(i);
        currentKeySet=appKeyStore.getCurrentKeySet();
        setKeysTextView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.m_add){
            AlertDialog.Builder builder=new AlertDialog.Builder(this);

            View v=View.inflate(this,R.layout.view_edit_key_set,null);
            final TextInputEditText et_pub, et_pri, et_name;
            et_pub = v.findViewById(R.id.et_pub);
            et_pri = v.findViewById(R.id.et_pri);
            et_name = v.findViewById(R.id.et_name);

            final RSAKeySet keySet= RSAUtils.genRSAKeySet();
            et_pub.setText(keySet.getPub());
            et_pri.setText(keySet.getPri());
            builder.setView(v);

            builder.setNegativeButton("取消",null).setPositiveButton("添加", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String pub = et_pub.getText().toString();
                    String pri = et_pri.getText().toString();
                    String keyName = et_name.getText().toString();

                    if (TextUtils.isEmpty(pub) || TextUtils.isEmpty(pri) || TextUtils.isEmpty(keyName)) {
                        Toast.makeText(RSAKeyManagerActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    RSAKeySet keySetNew=new RSAKeySet(keyName,pub,pri);
                    if(appKeyStore.add(keySetNew)){
                        Toast.makeText(RSAKeyManagerActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                        reload();
                    }else{
                        Toast.makeText(RSAKeyManagerActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.show();
        }else if(item.getItemId()==R.id.m_edit){

            AlertDialog.Builder builder=new AlertDialog.Builder(this);

            View v=View.inflate(this,R.layout.view_edit_key_set,null);
            final TextInputEditText et_pub, et_pri, et_name;
            et_pub = v.findViewById(R.id.et_pub);
            et_pri = v.findViewById(R.id.et_pri);
            et_name = v.findViewById(R.id.et_name);

            et_name.setText(currentKeySet.getName());
            et_pub.setText(currentKeySet.getPub());
            et_pri.setText(currentKeySet.getPri());

            builder.setView(v);

            builder.setNegativeButton("取消",null).setPositiveButton("更改", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String pub = et_pub.getText().toString();
                    String pri = et_pri.getText().toString();
                    String keyName = et_name.getText().toString();

                    if (TextUtils.isEmpty(pub) || TextUtils.isEmpty(pri) || TextUtils.isEmpty(keyName)) {
                        Toast.makeText(RSAKeyManagerActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    RSAKeySet keySetNew=new RSAKeySet(keyName,pub,pri);
                    if(appKeyStore.update(LocalSettingsUtils.readInt(getApplicationContext(),LocalSettingsUtils.FIELD_CURRENT_KEY_SET_INDEX),keySetNew)){
                        Toast.makeText(RSAKeyManagerActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                        reload();
                    }else{
                        Toast.makeText(RSAKeyManagerActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.show();

        }else if(item.getItemId()==R.id.m_backup){
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            final EditText et_pwd=new EditText(this);
            et_pwd.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(et_pwd);
            builder.setTitle("请设置备份密码");
            builder.setNegativeButton("取消",null).setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String pwd=et_pwd.getText().toString();
                    if(TextUtils.isEmpty(pwd)){
                        Toast.makeText(RSAKeyManagerActivity.this,"密码不能为空",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(appKeyStore.backup(pwd)){
                        Toast.makeText(RSAKeyManagerActivity.this,"备份成功",Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(RSAKeyManagerActivity.this,"备份失败",Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.show();
        }else if(item.getItemId()==R.id.m_recovery){
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            final EditText et_pwd=new EditText(this);
            et_pwd.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(et_pwd);
            builder.setTitle("请输入备份密码");
            builder.setNegativeButton("取消",null).setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String pwd=et_pwd.getText().toString();
                    if(TextUtils.isEmpty(pwd)){
                        Toast.makeText(RSAKeyManagerActivity.this,"密码不能为空",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(appKeyStore.recovery(pwd)){
                        Toast.makeText(RSAKeyManagerActivity.this,"恢复成功",Toast.LENGTH_SHORT).show();
                        reload();
                    }else{
                        Toast.makeText(RSAKeyManagerActivity.this,"恢复失败",Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.show();
        }else if(item.getItemId()==R.id.m_delete){
            if(appKeyStore.getKeySets().size()<=1){
                Toast.makeText(this,"不能删除",Toast.LENGTH_SHORT).show();
            }else{
                AlertDialog.Builder builder=new AlertDialog.Builder(this);
                builder.setTitle("是否删除");
                builder.setMessage("是否删除此密钥对（此操作不可逆）");
                builder.setNegativeButton("取消",null).setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(appKeyStore.remove(LocalSettingsUtils.readInt(getApplicationContext(),LocalSettingsUtils.FIELD_CURRENT_KEY_SET_INDEX))){
                            Toast.makeText(RSAKeyManagerActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
                            reload();
                        }else{
                            Toast.makeText(RSAKeyManagerActivity.this,"删除失败",Toast.LENGTH_SHORT).show();
                        }

                    }
                });
                builder.show();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void verifyStoragePermissions() {
        try {
            int permission = ActivityCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        "android.permission.READ_EXTERNAL_STORAGE",
                        "android.permission.WRITE_EXTERNAL_STORAGE"}, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_manage_key_set,menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
