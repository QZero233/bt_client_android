package com.nasa.bt;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import com.nasa.bt.fragments.GenCAFragment;
import com.nasa.bt.fragments.TrustedKeyFragment;
import com.nasa.bt.fragments.TrustedRemoteKeyFragment;

public class CACenterActivity extends AppCompatActivity {

    private Fragment genCaFragment,remoteKeyFragment,keyFragment;

    private BottomNavigationView bnv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ca_center);

        setTitle("证书安全中心");

        bnv=findViewById(R.id.bnv);
        bnv.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                FragmentTransaction transaction=getSupportFragmentManager().beginTransaction();
                hideAllFragment(transaction);

                switch (menuItem.getItemId()){
                    case R.id.m_gen_ca:
                        if(genCaFragment==null){
                            genCaFragment=new GenCAFragment();
                            transaction.add(R.id.fl,genCaFragment);
                        }else{
                            transaction.show(genCaFragment);
                        }
                        break;
                    case R.id.m_trusted_remote_key:
                        if(remoteKeyFragment==null){
                            remoteKeyFragment= new TrustedRemoteKeyFragment();
                            transaction.add(R.id.fl,remoteKeyFragment);
                        }else{
                            transaction.show(remoteKeyFragment);
                        }
                        break;
                    case R.id.m_trusted_key:
                        if(keyFragment==null){
                            keyFragment= new TrustedKeyFragment();
                            transaction.add(R.id.fl,keyFragment);
                        }else{
                            transaction.show(keyFragment);
                        }
                        break;
                }

                transaction.commit();
                return true;
            }
        });

        bnv.setSelectedItemId(R.id.m_trusted_remote_key);
    }

    private void hideAllFragment(FragmentTransaction transaction){
        if(genCaFragment!=null)
            transaction.hide(genCaFragment);
        if(remoteKeyFragment!=null)
            transaction.hide(remoteKeyFragment);
        if(keyFragment!=null)
            transaction.hide(keyFragment);
    }
}
