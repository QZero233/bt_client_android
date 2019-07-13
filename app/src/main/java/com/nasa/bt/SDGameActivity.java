package com.nasa.bt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class SDGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdgame);

        setTitle("严肃的数独游戏");
    }

    public void click(View v){
        Toast.makeText(this,"这是一个很严肃的游戏，请三思而后行",Toast.LENGTH_SHORT).show();
    }
}
