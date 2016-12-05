package com.fantom.vanger.musikcards;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DialogAudioList extends DialogFragment implements
        //SimpleCursorAdapter.ViewBinder,
        LoaderManager.LoaderCallbacks<Cursor>,
        DialogInterface.OnClickListener {

    public static final String TAG = "DialogAudioList";
    private static final String ARG_USER_LIST = "user_list";
    private ListView listView;
    private SimpleCursorAdapter adapterLv;
    private Set<String> userSet = new HashSet<>();
    private List<Integer> checkedPosition = new ArrayList<>();
    private boolean isListClicked = false;
    private int countSelected;

    public static DialogAudioList newInstance(String checkedItems) {
        Bundle args = new Bundle();
        args.putString(ARG_USER_LIST, checkedItems);

        DialogAudioList audioList = new DialogAudioList();
        audioList.setArguments(args);
        return audioList;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String checkedItems = getArguments().getString(ARG_USER_LIST);
        try {
            JSONArray arr = new JSONArray(checkedItems);
            countSelected = arr.length();
            for (int i = 0; i < countSelected; i++) checkedPosition.add(arr.getInt(i));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_list_audio, null);

        ((TextView) v.findViewById(R.id.count_selected)).setText(Integer.toString(countSelected));
        listView = (ListView) v.findViewById(R.id.dialog_list_audio);

        String[] from = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID};
        int[] to = {android.R.id.text1, android.R.id.text2};
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                getActivity(), android.R.layout.simple_list_item_multiple_choice, null, from, to, 0);
        //adapter.setViewBinder(this);
        adapterLv = adapter;
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                isListClicked = true;
                CursorAdapter adapter = (CursorAdapter) adapterView.getAdapter();
                Cursor c = (Cursor) adapter.getItem(position);
                int uriColumn = c.getColumnIndex(MediaStore.Audio.Media.DATA);

                SparseBooleanArray checked = ((ListView) adapterView).getCheckedItemPositions();
                userSet.clear();
                checkedPosition.clear();
                int j = 0;
                for (int i = 0; i < checked.size(); i++) {
                    if (checked.valueAt(i)) {
                        c = (Cursor) adapter.getItem(checked.keyAt(i));
                        userSet.add(c.getString(uriColumn));
                        checkedPosition.add(checked.keyAt(i));
                        j++;
                    }
                }
                //set TextView's Count
                ((TextView) ((ViewGroup) listView.getParent()).findViewById(R.id.count_selected)).setText(Integer.toString(j));
            }
        });

        getLoaderManager().initLoader(0, null, this);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.pickupsounds)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.clear_all, null)
                .setView(v)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (isListClicked) {
            SharedPreferences.Editor editSettings = getActivity().getSharedPreferences(SettingsActivity.PREFS_NAME, 0).edit();
            editSettings.putStringSet(SettingsActivity.AUDIO_LIST, userSet);

            JSONArray positions = new JSONArray();
            for (Integer j : checkedPosition) positions.put(j);
            editSettings.putString("checkedPosition", positions.toString());
            if (checkedPosition.size() == 0)
                editSettings.putBoolean(SettingsActivity.USER_CHOICE, false);

            editSettings.commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Button clearAll = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_NEUTRAL);
        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Integer j : checkedPosition) listView.setItemChecked(j, false);
                countSelected = 0;
                TextView count = (TextView) ((ViewGroup) listView.getParent()).findViewById(R.id.count_selected);
                count.setText("0");

                isListClicked = true;
                userSet.clear();
                checkedPosition.clear();
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return (new CursorLoader(
                getActivity().getApplication(),
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.DURATION + "<?", new String[]{"600000"},
                MediaStore.Audio.Media.ALBUM));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        adapterLv.swapCursor(c);
        if (checkedPosition.size() > 0) {
            for (Integer i : checkedPosition) listView.setItemChecked(i, true);
            listView.setSelection(checkedPosition.get(0));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CursorAdapter) adapterLv).swapCursor(null);
    }

/*    @Override
    public boolean setViewValue(View v, Cursor c, int column) {
        if (column == c.getColumnIndex(MediaStore.Video.Media._ID)) {
            Uri video=
                    ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            c.getInt(column));

            Picasso.with(getActivity()).load(video.toString())
                    .fit().centerCrop()
                    .placeholder(R.drawable.ic_media_video_poster)
                    .into((ImageView)v);
            return(true);
        }
        return(false);
    }

    interface Contract {
        void onVideoSelected(String uri, String mimeType);
    }*/
}

