package com.fantom.vanger.musikcards;

import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;


public class SettingsActivity extends AppCompatActivity {
    public static final String TAG = "Settings Activity";
    public static final String PREFS_NAME = "settingsFile";
    public static final String STRING_COLUMN = "columns";
    public static final String STRING_ROW = "rows";
    public static final String BOARD_SIZE = "boardSize";
    public static final String KIND_CARD = "kindCard";
    public static final String LEVEL_COUNT = "levelCount";
    public static final String AUDIO_LIST = "audioList";
    public static final String USER_CHOICE = "userList";
    public static final String TOTAL_SCORE = "totalScore";
    public static final String ASSETS_DB = "assetsDb";
    private static final String DIALOG_AUDIO_LIST = "MediaStore.Audio";
    private SharedPreferences settings;
    private static final int FILE_SELECT_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
/*
        int boardSize = settings.getInt(MusicCardsActivity.BOARD_SIZE,R.id.set_easy);
        //boardSize = R.id.set_easy;
        ((RadioButton)findViewById(boardSize)).setChecked(true);
*/
        settings = getSharedPreferences(PREFS_NAME, 0);
        int boardSize = settings.getInt(BOARD_SIZE, 1);
        ((Spinner) findViewById(R.id.set_boardsize)).setSelection(boardSize);

        int kindCard = settings.getInt(KIND_CARD, R.id.set_same);
        //kindCard = R.id.set_diff;
        ((RadioButton) findViewById(kindCard)).setChecked(true);

//        boolean userChoice = settings.getBoolean(MusicCardsActivity.USER_CHOICE,false);
//        ((CheckBox)findViewById(R.id.check_user_list)).setChecked(userChoice);

        findViewById(R.id.set_own).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getSupportFragmentManager();
                DialogAudioList dialog = DialogAudioList
                        .newInstance(settings.getString("checkedPosition", "[]"));
                dialog.show(fm, DIALOG_AUDIO_LIST);
            }
        });
    }

    @Override
    protected void onPause() {
        int x, y;
        super.onPause();
        RadioGroup rg = (RadioGroup) findViewById(R.id.set_kindcard);
        Spinner spinner = (Spinner) findViewById(R.id.set_boardsize);
        switch (spinner.getSelectedItemPosition()) {
            case 0:
                x = 2;
                y = 3;
                break;
            case 2:
                x = 3;
                y = 4;
                break;
            case 3:
                x = 4;
                y = 5;
                break;
            case 4:
                x = 5;
                y = 6;
                break;
            default:
            case 1:
                x = 2;
                y = 4;
        }
        SharedPreferences.Editor editor = settings.edit();
        if (rg.getCheckedRadioButtonId()==R.id.set_diff){   //
            if (x*y>24) {x=4;y=5;}
        }
        editor.putInt(STRING_COLUMN, x);
        editor.putInt(STRING_ROW, y);
        editor.putInt(BOARD_SIZE, spinner.getSelectedItemPosition());
        editor.putInt(KIND_CARD, rg.getCheckedRadioButtonId());
        //editor.putInt(MusicCardsActivity.LEVEL_COUNT, 2);
        //editor.putBoolean(MusicCardsActivity.USER_CHOICE,((CheckBox)findViewById(R.id.check_user_list)).isChecked());
        editor.commit();
    }

}
