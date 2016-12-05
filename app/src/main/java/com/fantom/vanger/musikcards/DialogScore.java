package com.fantom.vanger.musikcards;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Vanger on 29.11.2016.
 */

public class DialogScore extends DialogFragment implements DialogInterface.OnClickListener{
    public static final String TAG = "Dialog Score Table:";
    public static final String TOTAL_PAIR = "total Pair";
    public static final String TOTAL_ATTEMPTS = "total Attempts";

    private int totalPair,totalAttempts;

    public static DialogScore newInstace(int totalAttempts, int totalPair){
        Bundle bundle = new Bundle();
        bundle.putInt(TOTAL_PAIR,totalPair);
        bundle.putInt(TOTAL_ATTEMPTS,totalAttempts);

        DialogScore scoreDialog = new DialogScore();
        scoreDialog.setArguments(bundle);
        return scoreDialog;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        totalPair = getArguments().getInt(TOTAL_PAIR);
        totalAttempts = getArguments().getInt(TOTAL_ATTEMPTS);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v  = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_score,null);

        // Attempts: #countAttempts = countAttempts + Bingos count = 12 + 6 (etc..)
        // Score: (mAmount/2)*100/countAttempts = 60 (etc..
        // Total: total + Score(60)
        // Button: next/repeat (kick FAB)
        ((TextView)v.findViewById(R.id.text_attempts)).setText(Integer.toString(totalAttempts));

        int score = Math.round(totalPair*100/totalAttempts);
        ((TextView)v.findViewById(R.id.text_score)).setText(Integer.toString(score));

        SharedPreferences pref = getActivity().getSharedPreferences(SettingsActivity.PREFS_NAME,0);
        int totalScore = score + pref.getInt(SettingsActivity.TOTAL_SCORE,0);
        ((TextView)v.findViewById(R.id.text_total)).setText(Integer.toString(totalScore));
        pref.edit().putInt(SettingsActivity.TOTAL_SCORE,totalScore).commit();

        return new AlertDialog.Builder(getActivity())
                .setTitle("Bingo Maestro!")
                .setPositiveButton(R.string.next,this)
                .setNeutralButton(R.string.replay,null)
                .setView(v).create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {

    }
}
