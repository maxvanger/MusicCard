package com.fantom.vanger.musikcards;

/**
 * Created by Vanger on 22.11.2016.
 */

public class Album {
    private int mId;
    private String mTitle;
    private String mAuthor;
    private String mAbout;
    private String mImageUri;


    public int getId() { return mId; }

    public void setId(int id) { mId = id; }

    public String getTitle() { return mTitle; }

    public void setTitle(String title) { mTitle = title; }

    public String getAuthor() { return mAuthor; }

    public void setAuthor(String author) { mAuthor = author; }

    public String getAbout() { return mAbout; }

    public void setAbout(String about) { mAbout = about; }

    public String getImageUri() { return mImageUri; }

    public void setImageUri(String imageUri) { mImageUri = imageUri; }
}
