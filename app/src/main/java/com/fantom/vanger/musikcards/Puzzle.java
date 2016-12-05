package com.fantom.vanger.musikcards;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * Created by vanger on 08/10/16.
 */

public class Puzzle {
    private String mTitle;
    private Drawable mImageHint;
    private Bitmap mImagePuzzle;
    private String mSoundPath;
    private int mId;

    private String mPathType;
    private int mAlbumId;
    private int mLinkId;


    public Puzzle(String filepath){
        mSoundPath = filepath;
        getMusicTag();
    }

    Puzzle(Puzzle p){
        mTitle = p.getTitle();
        mImageHint = p.getImageHint();
        mSoundPath = p.getSoundPath();
        mId = p.getId();//*p.getId();

        mPathType = p.getPathType();
        mAlbumId = p.getAlbumId();
        mLinkId = p.getLinkId();
    }

    public String getTitle() {
        return mTitle;
    }

    private void getMusicTag(){
        //get music tag from MP3 file
        mTitle = "";
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public Bitmap getImagePuzzle() {
        return mImagePuzzle;
    }

    public void setImagePuzzle(Bitmap imagePuzzle) {
        mImagePuzzle = imagePuzzle;
    }

    public Drawable getImageHint() { return mImageHint; }

    public void setImageHint(Drawable imageHint) { mImageHint = imageHint; }

    public String getSoundPath() {
        return mSoundPath;
    }

    public void setSoundPath(String soundPath) {
        this.mSoundPath = soundPath;
    }

    public int getId() { return mId; }

    public void setId(int id) { mId = id; }

    public String getPathType() { return mPathType; }

    public void setPathType(String pathType) { mPathType = pathType; }

    public int getAlbumId() { return mAlbumId; }

    public void setAlbumId(int albumId) { mAlbumId = albumId; }

    public int getLinkId() { return mLinkId; }

    public void setLinkId(int linkId) { mLinkId = linkId; }
}
