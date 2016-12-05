package com.fantom.vanger.musikcards;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


import com.fantom.vanger.musikcards.db.PuzzleCursorWrapper;
import com.fantom.vanger.musikcards.db.PuzzleDbHelper;
import com.fantom.vanger.musikcards.db.PuzzleDbSchema;
import com.fantom.vanger.musikcards.db.PuzzleDbSchema.AlbumsTable;
import com.fantom.vanger.musikcards.db.PuzzleDbSchema.PuzzleTable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static android.R.attr.level;

/**
 * Created by vanger on 08/10/16.
 * <p>
 * This Class should take out next code:
 * from SoundBase class: loadSound(), prepareSound(), play(puzzle), pause()
 * from BoardFragment: fillCards, fillKeys, getPuzzleImage(img,x,y),Collection.shuffle
 * <p>
 * Also, this class should union the data objects:
 * List<lock>, List<keys>, List<text>, List<color> in one BoardBundle
 * <p>
 * = Initialize:
 * - newInstance(Context,R.layout.item_lock_view)
 * - get Store object
 * - get List(Map) of puzzle's objects from Store
 * <p>
 * = Wrapping mPuzzle to View
 * = Returning the View
 * = ? MediaPlayer.prepare()
 * = Playing the Music
 */

public class MixPresenterStore implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "MixPresenterStore";
    public static final int TYPE_LOCK_KEY = 0;
    public static final int TYPE_COUPLES = R.id.set_diff;
    public static final int TYPE_EQUAL = R.id.set_same;

    private AssetManager mAssets;
    private int mAmount;
    private int mXcount;
    private int mYcount;
    private final int mLevel;
    private int mKindCards;
    private Album mAlbum;
    private Activity parentActivity;
    private Map<Puzzle, Puzzle> mPuzzleMap = new HashMap<>();
    private Map<Integer, Integer> mPuzzleIdMap = new HashMap<>();
    private Map<Integer, MediaPlayer> soundPrepared = new HashMap<>();
    public static List<Bitmap> hintImagesList;
    public Bitmap[] hintImages;
    private Bitmap mPuzzleImage;
    private boolean isUserChoice = false;
    private List<String> audioList;
    private List<Integer> mColorList;
    private Context ctx;

    private Random rnd;
    private List<Puzzle> mPuzzleList;

    public interface PuzzleListLoader {
        void onLoadPuzzleList(List<Puzzle> puzzleList);
    }
/*    public static MixPresenterStore newInstance(Activity activity, int countX, int countY, int page) {
        return new MixPresenterStore(activity, countX, countY, page);
    }*/

    MixPresenterStore(Activity activity, int x, int y, int kindCards) {
        ctx = activity;
        mXcount = x;
        mYcount = y;
        mAmount = x * y;
        mLevel = level;
        mKindCards = kindCards;
        mAssets = ctx.getAssets();

        rnd = new Random();
        hintImages = getHintImages();

/*        //Generate Map<int,int> puzzleIdMap
        Puzzle p = null;
        for (Puzzle key : mPuzzleMap.keySet()) {
            if (mPuzzleMap.containsKey(key)) {
                p = mPuzzleMap.get(key);
            }
            mPuzzleIdMap.put(p.getId(), key.getId());
        }*/
    }

    //Store interface now implement in the current class
    //Mapping Key-To-Lock(puzzles) use in case (KindCard==the_diff)
    private Map<Puzzle, Puzzle> getPuzzleMap() {
        return null;
    }

    //choosing the image(from Assets) for puzzling according album.getImageUri(return imagefolder(?or concrete Image) for album)
    private Bitmap getRandomBitmap() {
        Bitmap bitmap = null;
        try {
            String path;
            if (mKindCards == R.id.set_own) path = "0_extra/images";
            else path = mAlbum.getImageUri();
            String[] files = mAssets.list(path);
            int i = rnd.nextInt(files.length);
            InputStream stream = mAssets.open(path + "/" + files[i]);
            bitmap = BitmapFactory.decodeStream(stream);
        } catch (IOException e) {
            Log.e(TAG, "assets.list (getRandombitmap) error");
        }
        return bitmap;
    }

    //get hint images from Assets folder 0_extra/hints/
    private Bitmap[] getHintImages() {
        Bitmap[] hintImagesArray = new Bitmap[10];
        try {
            String[] files = mAssets.list("0_extra/hints");
            hintImagesArray = new Bitmap[files.length];
            int i = 0;
            for (String fname : files) {
                InputStream stream = mAssets.open("0_extra/hints/" + fname);
                hintImagesArray[i++] = BitmapFactory.decodeStream(stream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hintImagesArray;
    }

    private Album getAlbum(int albumId) {
        PuzzleDbHelper dbHelper = new PuzzleDbHelper(ctx);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(AlbumsTable.TABLE_NAME, null,
                AlbumsTable.ID + "=?", new String[]{Integer.toString(albumId)},
                null, null, null, "1");
        c.moveToFirst();
        Album album = new Album();
        album.setId(c.getInt(c.getColumnIndex(AlbumsTable.ID)));
        album.setTitle(c.getString(c.getColumnIndex(AlbumsTable.TITLE)));
        album.setAuthor(c.getString(c.getColumnIndex(AlbumsTable.AUTHOR)));
        album.setAbout(c.getString(c.getColumnIndex(AlbumsTable.ABOUT)));
        album.setImageUri(c.getString(c.getColumnIndex(AlbumsTable.IMAGE_URI)));
        c.close();
        return album;
    }

    public void loadPuzzles(int albumId, int kindCards) {
        Uri loaderUri = null;
        String whereString = null, whereArgString = null;
        String orderString = "RANDOM() LIMIT " + mAmount / 2;
        mAlbum = getAlbum(albumId);

        switch (kindCards) {
            case R.id.set_own:
                //TODO CursorLoader from MediaStore.Audio
                SharedPreferences pref = ctx.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
                Set<String> userChoiceSet = pref.getStringSet(SettingsActivity.AUDIO_LIST, null);
                StringBuilder userChoice = new StringBuilder("(");
                //Making string "(path_1,path_2,path_3,path_N)" for following WHERE_ARGS
                for (String s : userChoiceSet) {
                    userChoice.append("\"").append(s).append("\"").append(",");
                }
                userChoice.deleteCharAt(userChoice.length() - 1);
                userChoice.append(")");

                whereString = MediaStore.Audio.Media.DATA + " IN " + userChoice.toString();
                loaderUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                break;

            case TYPE_LOCK_KEY:
                orderString = "RANDOM() LIMIT " + mAmount;
            case R.id.set_diff:
            case R.id.set_same:
                Uri uri = Uri.parse(PuzzleDbSchema.CONTENT_URI + PuzzleDbSchema.PuzzleTable.TABLE_NAME);
                loaderUri = Uri.withAppendedPath(uri, Integer.toString(albumId));
                if (kindCards != R.id.set_same)
                    loaderUri = Uri.withAppendedPath(loaderUri, "pairs"); //URI .../puzzle/[albumId]/pairs
            default:
        }
        Bundle loaderBundle = new Bundle();
        loaderBundle.putString("URI", loaderUri.toString());
        loaderBundle.putString("WHERE", whereString);
        loaderBundle.putString("WHERE_ARG", whereArgString);
        loaderBundle.putString("ORDER", orderString);

        if (((AppCompatActivity) ctx).getSupportLoaderManager().getLoader(kindCards) == null)
            ((AppCompatActivity) ctx).getSupportLoaderManager().initLoader(kindCards, loaderBundle, this);
        else
            ((AppCompatActivity) ctx).getSupportLoaderManager().restartLoader(kindCards, loaderBundle, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
/*        switch(id){
            case R.id.set_own:
            case R.id.set_same:
            case R.id.set_diff:
        }*/
        return (new CursorLoader(
                ctx.getApplicationContext(),
                Uri.parse(args.getString("URI")),
                null,
//                null,null,
                args.getString("WHERE"),
                //               new String[]{args.getString("WHERE_ARG")},
                null,
                args.getString("ORDER")
        ));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mPuzzleList = new ArrayList<>();
        int typeOfCard = loader.getId();
        switch (typeOfCard) {
            case R.id.set_own:
                cursor.moveToFirst();
                int countId = 1000;
                while (!cursor.isAfterLast()) {
                    Puzzle p = new Puzzle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                    p.setPathType("uri");
                    p.setId(countId++);
                    p.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                    mPuzzleList.add(p);
                    cursor.moveToNext();
                }
                break;
            case R.id.set_same:
            case R.id.set_diff:
            case TYPE_LOCK_KEY:
                PuzzleCursorWrapper curWrap = new PuzzleCursorWrapper(cursor);
                curWrap.moveToFirst();
                while (!curWrap.isAfterLast()) {
                    mPuzzleList.add(curWrap.getPuzzle());
                    curWrap.moveToNext();
                }
        }

        if (typeOfCard == R.id.set_same || typeOfCard == R.id.set_own) {
            //adding a copy of each Puzzle to puzzleList AND set hintImage
            List<Puzzle> copies = new ArrayList<>();
            int i = 0;
            Drawable drw;
            for (Puzzle p : mPuzzleList) {
                drw = new BitmapDrawable(hintImages[i % hintImages.length]);
                p.setImageHint(drw);
                Puzzle same = new Puzzle(p);
                copies.add(same);
                i++;
            }
            mPuzzleList.addAll(copies);
        } else if (typeOfCard == R.id.set_diff || typeOfCard == TYPE_LOCK_KEY) {
            //set hintImage for lock-and-keys (type of) puzzle
            int i = 0, j = 0, resId;
            for (Puzzle p : mPuzzleList) {
//                if (i%2 ==0 ) resId = R.drawable.button_lock_normal;
//                else resId = R.drawable.button_key_normal;
                Drawable drw = getShapeDrawable(R.drawable.button_lock_over, mColorList.get(j%mColorList.size()));
                p.setImageHint(drw);
                if (i % 2 != 0) j++;
                i++;}
        }

        // Set the puzzled Image to each Puzzle
        List<Puzzle> keysList = new ArrayList<>();
        if (typeOfCard == TYPE_LOCK_KEY) {
            for(Puzzle p:mPuzzleList){
                if (p.getLinkId() != -1) keysList.add(p);
            }
            mPuzzleList.removeAll(keysList);
            setPuzzledImage();
            mPuzzleList.addAll(keysList);
            for (Puzzle p:keysList){
                try {
                    prepareSound(p);
                } catch (IOException e) { e.printStackTrace(); }
            }
        } else setPuzzledImage();

        //return puzzleList;
        ((PuzzleListLoader) ctx).onLoadPuzzleList(mPuzzleList);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }


    public void setPuzzledImage() {
        Collections.shuffle(mPuzzleList);
        mPuzzleImage = getRandomBitmap();
        int i = 0;
        int dX = mPuzzleImage.getWidth() / mXcount;
        int dY = mPuzzleImage.getHeight() / mYcount;
        for (Puzzle puzzle : mPuzzleList) {
            puzzle.setImagePuzzle(getPuzzleImage(mPuzzleImage, i, dX, dY));
            try {
                prepareSound(puzzle);
            } catch (IOException e) {
                Log.e(TAG, "Error with MediaPlayer's preparing");
            }
            i++;
        }
    }

    public void reset() {
        for (MediaPlayer mp : soundPrepared.values()) {
            mp.release();
        }
        soundPrepared.clear();
    }

    private void prepareSound(Puzzle puzzle) throws IOException {
        if (soundPrepared.containsKey(puzzle.getId())) return;
        MediaPlayer mp = new MediaPlayer();
        if (puzzle.getPathType().contentEquals("assets")) {
            AssetFileDescriptor afd = mAssets.openFd(puzzle.getSoundPath());
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } else if (puzzle.getPathType().contentEquals("uri")) {
            mp.setDataSource(ctx, Uri.parse(puzzle.getSoundPath()));
        }
        mp.prepare();
        soundPrepared.put(puzzle.getId(), mp);
    }

    public void play(int soundId) {
        if (soundPrepared.containsKey(soundId)) {
            MediaPlayer mp = soundPrepared.get(soundId);
            mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            mp.seekTo(0);
        }
    }

    public void pause(int soundId) {
        if (soundPrepared.containsKey(soundId)) {
            MediaPlayer mp = soundPrepared.get(soundId);
            mp.pause();
        }
    }

    public void releaseMP(int soundId) {
        if (soundPrepared.containsKey(soundId)) {
            MediaPlayer mp = soundPrepared.get(soundId);
            mp.release();
        }
    }

    private Bitmap getPuzzleImage(Bitmap source, int count, int sizeX, int sizeY) {
        int x = count % mXcount;
        int y = count / mXcount;
        Bitmap result = Bitmap.createBitmap(source, x * sizeX, y * sizeY, sizeX, sizeY);
        return result;
    }

    public float getImageRatio() {
        return (float) mPuzzleImage.getWidth() / (float) mPuzzleImage.getHeight();
    }

    public void setColorsHints(List<Integer> colorList){
        mColorList = colorList;
    }

    private Drawable getShapeDrawable(int drawable, int color) {
        Drawable dr = ctx.getResources().getDrawable(drawable);
        if (dr instanceof ShapeDrawable) {
            ((ShapeDrawable)dr).getPaint().setColor(color);
        } else if (dr instanceof GradientDrawable) {
            ((GradientDrawable)dr).setColor(color);
        }
        return dr;
    }

    public void assetsInsertToProvider(){
        String LOCKS_FOLDER = "/locks";
        String KEYS_FOLDER = "/keys";
        ContentValues cv = new ContentValues();
        ContentResolver cr = ctx.getContentResolver();
        Uri albumUri = Uri.parse(PuzzleDbSchema.CONTENT_URI+AlbumsTable.TABLE_NAME);
        Uri puzzleUri = Uri.parse(PuzzleDbSchema.CONTENT_URI+PuzzleTable.TABLE_NAME);
        try{
            String[] dirs = mAssets.list("");
            for(int d=1;d<3;d++){ //d<dirs.length - But filter assets folder (bgin from digit)
                // insert new Album from Assets directory
                String[] albumInfo = dirs[d].split("_");
                cv.put(AlbumsTable.AUTHOR,albumInfo[1]);
                cv.put(AlbumsTable.TITLE,albumInfo[2]);
                cv.put(AlbumsTable.IMAGE_URI,dirs[d]+"/images");
                Uri uriAlbum = cr.insert(albumUri,cv);
                long albumId = ContentUris.parseId(uriAlbum);

                // insert new puzzles for the current Album
                String[] locks = mAssets.list(dirs[d]+LOCKS_FOLDER);
                String[] keys = mAssets.list(dirs[d]+KEYS_FOLDER);
                for(String lock:locks){
                    cv.clear();
                    cv.put(PuzzleTable.ALBUM,Long.toString(albumId)); //ALBUM_ID
                    cv.put(PuzzleTable.LINK,"-1");   // Its a Lock Puzzle
                    cv.put(PuzzleTable.PATH,dirs[d]+LOCKS_FOLDER+"/"+lock);
                    cv.put(PuzzleTable.PATH_TYPE,"assets");
                    cv.put(PuzzleTable.TITLE,getNameString(lock));
                    Uri uriPuzzle = Uri.parse(puzzleUri+"/"+Long.toString(albumId));
                    Uri newPuzzleUri = cr.insert(uriPuzzle,cv);
                    long lockId = ContentUris.parseId(newPuzzleUri);

                    for(String key:keys){
                        if (key.startsWith(lock.substring(0,2))) {
                            cv.clear();
                            cv.put(PuzzleTable.ALBUM,Long.toString(albumId)); //ALBUM_ID
                            cv.put(PuzzleTable.LINK,Long.toString(lockId));   // Its a key Puzzle
                            cv.put(PuzzleTable.PATH,dirs[d]+KEYS_FOLDER+"/"+key);
                            cv.put(PuzzleTable.PATH_TYPE,"assets");
                            cv.put(PuzzleTable.TITLE,getNameString(key));
                            cr.insert(uriPuzzle,cv);
                        }
                    }
                }
                cv.clear();
            }
        } catch (IOException ioe) {Log.e(TAG,"insertAssetsProvider() error");}
    }
    private String getNameString(String filename){
        String res = filename.split("\\.")[0];
        StringBuilder result = new StringBuilder(res.length());
        for (int i = 0; i < res.length(); i++) {
            char c = res.charAt(i);
            if (c>'9'||c==' '||c=='-') {
                result.append(c);
            }
        }
        return result.toString();
        //return res;
    }
}
