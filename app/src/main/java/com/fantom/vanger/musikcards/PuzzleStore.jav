package com.fantom.vanger.musikcards;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.fantom.vanger.soundsmem.db.PuzzleCursorWrapper;
import com.fantom.vanger.soundsmem.db.PuzzleDbSchema;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.fantom.vanger.soundsmem.db.PuzzleDbSchema.AlbumsTable;
import com.fantom.vanger.soundsmem.db.PuzzleDbSchema.PuzzleTable;
/**
 * Created by vanger on 06/10/16.
 *
 *  Class which content the mediadata.
 *  Can collect at List or Map the media files(mp3,jpg) from localStorage, web, db.
 *  web: JSON
 *  localStorage: Assets(/Level#i/*.jpg; /Level#i/locks|keys/*.mp3)
 *  db: ? ADO?
 *
 *  = Initialize:
 *      Assets scan
 *      TODO: database
 *  = ? prepareSounds ?
 *  = downloading new SoundsLevel in background
 *
 *  Class Puzzle(String hint,ImagePuzzle,SoundPuzzle);
 */

public class PuzzleStore /*implements Presenter.Store*/ {

    public static final String TAG = "PuzzleStore:";
    public static String BASE_FOLDER="2_seasons";
    public static final String LOCKS_FOLDER="/locks";
    public static final String KEYS_FOLDER="/keys";
    public static final String IMAGE_FOLDER="/images";
    public static final int baseId = 1111;

    private int mAmount;
    private int mLevel;
    private List<Puzzle> mLocks,mKeys;
    private List<String> listLocks, listKeys;
    private Map<Puzzle,Puzzle> mPuzzleMap;

    private Context mContext;
    private AssetManager mAssets;
    Random rnd;

    public static PuzzleStore newInstance(Context ctx, int amount, int level){
        return new PuzzleStore(ctx, amount,level);
    }

    PuzzleStore(Context ctx, int amount, int level){
        mContext = ctx;
        mAmount = amount;
        mLevel = level;
        rnd = new Random();

        mAssets = mContext.getAssets();
        try {
            BASE_FOLDER = mAssets.list("")[level];
        } catch (IOException e) {
            e.printStackTrace();
        }
        //mPuzzleMap = getLocksKeys(mAmount);
        //assetsInsertToProvider();
    }

    public List<Puzzle> getPuzzles(String album,int amount){
        final List<Puzzle> puzzles = new ArrayList<>(amount);
        switch(album){
            case "SharedPref":
                SharedPreferences pref = mContext.getSharedPreferences(MusicCardsActivity.PREFS_NAME,0);
                Set<String> userChoiceSet = pref.getStringSet(MusicCardsActivity.USER_CHOICE,null);
                StringBuilder userChoice=new StringBuilder("(");
                //Making string "(path_1 | path_2 | path_3 | path_N)" for following WHERE_ARGS
                for(String s:userChoiceSet) {
                    userChoice.append(new String[]{s,"|"});
                }
                userChoice.deleteCharAt(userChoice.length()-1);
                userChoice.append(")");
                final String userChoiceString = userChoice.toString();

                ((AppCompatActivity)mContext).getSupportLoaderManager()
                        .initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
                    @Override
                    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
                        return (new CursorLoader(
                                mContext.getApplicationContext(),
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                                MediaStore.Audio.Media.DATA + "=?", new String[] {userChoiceString},
                                MediaStore.Audio.Media.ALBUM
                        ));
                    }

                    @Override
                    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
                        puzzles.clear();
                        c.moveToFirst();
                        int countId = 1000;
                        while(!c.isAfterLast()){
                            Puzzle p = new Puzzle(c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
                            p.setPathType("uri");
                            p.setId(countId++);
                            p.setTitle(c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                            puzzles.add(p);
                            c.moveToFirst();
                        }
                    }

                    @Override public void onLoaderReset(Loader<Cursor> loader) {}
                });
                break;
            case "assets":
                ((AppCompatActivity)mContext).getSupportLoaderManager().
                        initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
                    @Override
                    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                        Uri uri = Uri.parse(PuzzleDbSchema.CONTENT_URI+PuzzleTable.TABLE_NAME);
                        Uri.withAppendedPath(uri,Integer.toString(mLevel));
                        return (new CursorLoader(
                                mContext.getApplicationContext(),
                                uri,null,null,null,null
                        ));
                    }

                    @Override
                    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
                        puzzles.clear();
                        PuzzleCursorWrapper cw = new PuzzleCursorWrapper(c);
                        cw.moveToFirst();
                        while(!cw.isAfterLast()){
                            puzzles.add(cw.getPuzzle());
                            cw.moveToNext();
                        }
                    }

                    @Override
                    public void onLoaderReset(Loader<Cursor> loader) {

                    }
                });

            default:
        }
        return puzzles;
    }

    public void assetsInsertToProvider(){
        ContentValues cv = new ContentValues();
        ContentResolver cr = mContext.getContentResolver();
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


    public Map<Puzzle,Puzzle> getLocksKeys(int n){
        Map <Puzzle,Puzzle> map = new HashMap<>();
        String[] locks,keys;
        List<String> locksList;
        List<String> rightKeys = new ArrayList<>();

        try{
            locks = mAssets.list(BASE_FOLDER + LOCKS_FOLDER);
            keys = mAssets.list(BASE_FOLDER + KEYS_FOLDER);
            locksList = Arrays.asList(locks);
            //Collections.shuffle(locksList);

            for(int i=0;i<n;i++){
                String lock = locksList.get(i);
                Puzzle keyPuzzle;
                if (locks.length!=keys.length) {
                    for(String key:keys){
                        if (key.startsWith(lock.substring(0,2))) {rightKeys.add(key);}
                    }
                    int r = rnd.nextInt(rightKeys.size());
                    keyPuzzle = new Puzzle(BASE_FOLDER+KEYS_FOLDER+"/"+rightKeys.get(r));
                    keyPuzzle.setTitle(getNameString(rightKeys.get(r)));
                }else{
                    keyPuzzle = new Puzzle(BASE_FOLDER+KEYS_FOLDER+"/"+keys[i]);
                    keyPuzzle.setTitle(getNameString(keys[i]));
                }
                keyPuzzle.setId(baseId+n+i);

                Puzzle lockPuzzle = new Puzzle(BASE_FOLDER+LOCKS_FOLDER+"/"+locks[i]);
                lockPuzzle.setId(baseId+i);
                lockPuzzle.setTitle(getNameString(locks[i]));

                map.put(lockPuzzle,keyPuzzle);
                rightKeys.clear();
            }
        } catch (IOException e) {Log.e(TAG,"assets.list() error");}

        return map;
    }

    public Map<Puzzle, Puzzle> getPuzzleMap() {
        return mPuzzleMap;
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

    public Bitmap getRandomBitmap(){
        Bitmap bitmap=null;
        try{
            String[] files = mAssets.list(BASE_FOLDER+IMAGE_FOLDER);
            int i = (int)(Math.random()*100)%files.length;
            i = rnd.nextInt(files.length);
            InputStream stream = mAssets.open(BASE_FOLDER+IMAGE_FOLDER+"/"+files[i]);
            bitmap = BitmapFactory.decodeStream(stream);
        } catch (IOException e){Log.e(TAG, "assets.list (for JPG) error");}
        return bitmap;
    }

    public List<Bitmap> getHintImages(){
        List<Bitmap> hintImages = new ArrayList<>();
        try {
            String[] files = mAssets.list("0_extra/hints");
            for(String fname:files) {
                InputStream stream = mAssets.open("0_extra/hints/"+fname);
                hintImages.add(BitmapFactory.decodeStream(stream));
            }
        } catch (IOException e) { e.printStackTrace(); }
        return hintImages;
    }

    public AssetManager getAssets() { return mAssets; }


/*    private void prepareSound(SoundPuzzle soundPuzzle) throws IOException {
        AssetFileDescriptor afd = mAssets.openFd(soundPuzzle.getPath());
        MediaPlayer mp = new MediaPlayer();
        mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
        afd.close();
        mp.prepare();
        soundPuzzle.setSoundId(count);
        soundPrepared.put(count,mp);
        count++;*/
    }

/*    public void release(){
        for(int i=0;i<soundPrepared.size();i++){
            soundPrepared.get(i).release();
        }
    }

}*/
