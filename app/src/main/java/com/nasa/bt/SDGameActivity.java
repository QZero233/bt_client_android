package com.nasa.bt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.nasa.bt.utils.LocalSettingsUtils;

public class SDGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdgame);

        setTitle("严肃的数独游戏");

        if(LocalSettingsUtils.readInt(this,LocalSettingsUtils.FIELD_SD_GAME)==1)
            LocalSettingsUtils.saveInt(this,LocalSettingsUtils.FIELD_SD_GAME,0);
        else
            LocalSettingsUtils.saveInt(this,LocalSettingsUtils.FIELD_SD_GAME,1);
    }

    public void click(View v){
        Toast.makeText(this,"这是一个很严肃的游戏，请三思而后行",Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sd_game, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId()==R.id.m_quit){
            if(LocalSettingsUtils.readInt(this,LocalSettingsUtils.FIELD_SD_GAME)==1){
                int i=0;
                i=1/0;
            }else
                finish();

        }

        return false;
    }

}
