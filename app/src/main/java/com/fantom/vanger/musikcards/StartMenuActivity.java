package com.fantom.vanger.musikcards;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

//import com.bumptech.glide.Glide;

public class StartMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_menu);

/*       ImageView gif = (ImageView) findViewById(R.id.gif_anim);
        Glide.with(this)
                .load(R.drawable.startimage)
                .centerCrop()
                .into(gif);*/

        findViewById(R.id.main_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(StartMenuActivity.this,SettingsActivity.class);
                startActivity(i);
            }
        });

        findViewById(R.id.main_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i;
                SharedPreferences pref = getSharedPreferences(SettingsActivity.PREFS_NAME,0);
                final int typeCard = pref.getInt(SettingsActivity.KIND_CARD,R.id.set_same);
                if (typeCard == R.id.set_diff) {
                    i = new Intent(StartMenuActivity.this,MusicPairsActivity.class);
                } else {
                    i = new Intent(StartMenuActivity.this,MusicCardsActivity.class);
                }
                startActivity(i);
            }
        });
    }
}
