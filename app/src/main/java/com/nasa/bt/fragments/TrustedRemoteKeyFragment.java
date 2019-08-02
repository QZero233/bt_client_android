package com.nasa.bt.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nasa.bt.R;
import com.nasa.bt.data.dao.CADao;
import com.nasa.bt.data.entity.TrustedRemotePublicKeyEntity;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;

import java.util.List;

public class TrustedRemoteKeyFragment extends Fragment implements AdapterView.OnItemLongClickListener {

    private ListView lv_keys;
    private List<TrustedRemotePublicKeyEntity> keyEntityList;
    private CADao caDao;
    private Context context;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context=getActivity();

        View v=inflater.inflate(R.layout.fragment_trusted_remote_key,container,false);
        lv_keys=v.findViewById(R.id.lv_keys);
        lv_keys.setOnItemLongClickListener(this);

        caDao=new CADao(getActivity());
        loadKeys();

        return v;
    }

    private void loadKeys(){
        keyEntityList=caDao.getAllTrustedRemoteKeys();

        BaseAdapter adapter=new BaseAdapter() {
            @Override
            public int getCount() {
                if(keyEntityList==null || keyEntityList.isEmpty())
                    return 0;
                return keyEntityList.size();
            }

            @Override
            public Object getItem(int i) {
                return null;
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                TrustedRemotePublicKeyEntity keyEntity=keyEntityList.get(i);

                TextView tv=new TextView(context);
                tv.setText("IP:"+keyEntity.getIp()+"\nHash:"+keyEntity.getPublicKeyHash());

                return tv;
            }
        };

        lv_keys.setAdapter(adapter);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view,final int pos, long l) {
        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        builder.setTitle("是否删除此公钥？");
        builder.setMessage("将在下次连接时生效");
        builder.setNegativeButton("取消",null).setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                TrustedRemotePublicKeyEntity keyEntity=keyEntityList.get(pos);
                if(caDao.deleteTrustedRemoteKey(keyEntity.getIp())){
                    Toast.makeText(context,"删除成功，正在重连",Toast.LENGTH_SHORT).show();
                    MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_RECONNECT);
                    getActivity().finish();
                }else{
                    Toast.makeText(context,"删除失败",Toast.LENGTH_SHORT).show();
                }
            }
        }).show();

        return true;
    }
}
