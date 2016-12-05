package com.fantom.vanger.musikcards;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vanger on 08/10/16.
 *
 * This Class should take out next code:
 *  from SoundBase class: loadSound(), prepareSound(), play(puzzle), pause()
 *  from BoardFragment: fillCards, fillKeys, getPuzzleImage(img,x,y),Collection.shuffle
 *
 * Also, this class should union the data objects:
 *  List<lock>, List<keys>, List<text>, List<color> in one BoardBundle
 *
 *  = Initialize:
 *      - newInstance(Context,R.layout.item_lock_view)
 *      - get Store object
 *      - get List(Map) of puzzle's objects from Store
 *
 *  = Wrapping mPuzzle to View
 *  = Returning the View
 *  = ? MediaPlayer.prepare()
 *  = Playing the Music
 */

public class Presenter implements PuzzleBoardFragment.PuzzleBoard {
    public static final String TAG = "PuzzlePresenter";
    public static final int TYPE_LOCK = 0;
    public static final int TYPE_KEY = 1;
    public static final int TYPE_COUPLES = R.id.set_diff;
    public static final int TYPE_EQUAL = R.id.set_same;

    public interface Store  {
        Map<Puzzle,Puzzle> getPuzzleMap();
        Bitmap getRandomBitmap();
        List<Bitmap> getHintImages();
        AssetManager getAssets();
    }

    private Store mStore;
    private int mAmount;
    private int mXcount;
    private int mYcount;
    private Activity parentActivity;
    private Map<Puzzle,Puzzle> mPuzzleMap = new HashMap<>();
    private Map<Integer,Integer> mPuzzleIdMap = new HashMap<>();
    private Map<Integer,MediaPlayer> soundPrepared = new HashMap<>();
    public static List<Bitmap> hintImagesList;
    private Bitmap mPuzzleImage;
    private boolean isUserChoice = false;
    private List<String> audioList;
    private Context ctx;


    public static Presenter newInstance(Activity activity, int countX, int countY, int page) {
        return new Presenter(activity, countX, countY, page);
    }

    public static Presenter newInstance(Activity activity, int countX, int countY, int page, List<String> audioList) {
        return new Presenter(activity, countX, countY, page, audioList);
    }
    Presenter(Activity activity, int x, int y,int page,List<String> audioList){
        this(activity,x,y,page);
        isUserChoice = true;
        this.audioList = audioList;
    }

    Presenter(Activity activity, int x, int y,int page){
        ctx = activity;
        mXcount = x;
        mYcount = y;
        mAmount = x*y;

        mStore = PuzzleStore.newInstance(ctx,mAmount,page);
        mPuzzleMap = mStore.getPuzzleMap();
        mPuzzleImage = mStore.getRandomBitmap();
        hintImagesList = mStore.getHintImages();
/*        for(Map.Entry<Puzzle,Puzzle> entry:mPuzzleMap.entrySet()){
            mPuzzleIdMap.put(entry.getKey().getId(),entry.getValue().getId());
        }*/
        Puzzle p=null;
        for(Puzzle key:mPuzzleMap.keySet()){
            if (mPuzzleMap.containsKey(key)) {
                p = mPuzzleMap.get(key);
            }
            mPuzzleIdMap.put(p.getId(),key.getId());
        }
    }

    public List<Puzzle> getPuzzles(int type){
        Collection<Puzzle> list;
        List<Puzzle> puzzleList = new ArrayList<>();
        switch(type){
            case TYPE_LOCK:
                list = mPuzzleMap.keySet();
                break;
            case TYPE_KEY:
                list = mPuzzleMap.values();
                break;
            case TYPE_EQUAL:
            case R.id.set_own:
            case TYPE_COUPLES:
                int i=0;
                for(Puzzle key:mPuzzleMap.keySet()){
                    key.setImageHint(hintImagesList.get(i));
                    puzzleList.add(key);

                    if (type==TYPE_COUPLES) {
                        mPuzzleMap.get(key).setImageHint(hintImagesList.get(i));
                        puzzleList.add(mPuzzleMap.get(key));
                    } else {
                        Puzzle thesame = new Puzzle(key);
                        puzzleList.add(thesame);
                    }

                    i++;
                    if (i>=mAmount/2) break;
                }
            default:
                list = new ArrayList<>();
        }

        // govnocode
        if (isUserChoice) {
            int j = 0;
            for (Puzzle p : puzzleList) {
                p.setSoundPath(audioList.get(j));
                j++;
            }
        } else {
                for(Puzzle p:list) puzzleList.add(p);
        }
        // end govnocode

        Collections.shuffle(puzzleList);
        int i=0;
        int dX=mPuzzleImage.getWidth()/mXcount;
        int dY=mPuzzleImage.getHeight()/mYcount;
        for(Puzzle puzzle:puzzleList){
            puzzle.setImagePuzzle(getPuzzleImage(mPuzzleImage,i,dX, dY));
            try { prepareSound(puzzle);
            } catch (IOException e) { Log.e(TAG,"Error with MediaPlayer's preparing");}
            i++;
        }
        return puzzleList;
    }

/*    private View wrappingView(Puzzle puzzle, int layoutId){
        View fL = parentActivity.getLayoutInflater().inflate(layoutId,,null);
        return
    }*/

    public boolean check(Integer value,Integer key) {
        return (mPuzzleIdMap.containsKey(key)&&((int)value == (int)mPuzzleIdMap.get(key)));
    }

    public int getLockId(Integer keyId){
        if (mPuzzleIdMap.containsKey(keyId)){
            return mPuzzleIdMap.get(keyId);
        }
        return -1;


    }

    private void prepareSound(Puzzle puzzle) throws IOException {
        MediaPlayer mp = new MediaPlayer();
        if (!isUserChoice) {
            AssetFileDescriptor afd = mStore.getAssets().openFd(puzzle.getSoundPath());
            mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
            afd.close();
        } else {
            mp.setDataSource(ctx, Uri.parse(puzzle.getSoundPath()));
        }
        mp.prepare();
        soundPrepared.put(puzzle.getId(),mp);
    }

    public void play(int soundId){
        if (soundPrepared.containsKey(soundId)) {
            MediaPlayer mp=soundPrepared.get(soundId);
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
            MediaPlayer mp=soundPrepared.get(soundId);
            mp.pause();
        }
    }

    public void releaseMP(int soundId){
        if (soundPrepared.containsKey(soundId)) {
            MediaPlayer mp=soundPrepared.get(soundId);
            mp.release();
        }
    }

    private Bitmap getPuzzleImage(Bitmap source, int count, int sizeX, int sizeY) {
        int x = count % mXcount;
        int y = count / mXcount;
        Bitmap result = Bitmap.createBitmap(source, x * sizeX, y * sizeY, sizeX, sizeY);
        return result;
    }


    public float getImageRatio(){
        return (float)mPuzzleImage.getWidth()/(float)mPuzzleImage.getHeight();
    }

}
