/*
package com.q-artz.vanger.musikcards;


import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.percent.PercentFrameLayout;
import android.support.percent.PercentLayoutHelper;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;

*/
/**
 * MusicMem a simple memory game.
 *//*

public class MusicCardsFragment extends Fragment {
    private int amount;
    private int mXcount;
    private int mYcount;

    private SoundBase mSoundBase;
    private ArrayList<SoundPuzzle> mCards;
    private SoundPuzzle mCurrentCard;
    private int mCurrentIndex;
    private GridLayout grid;


    public static MusicCardsFragment newInstance(){
        return new MusicCardsFragment();
    }

    public MusicCardsFragment() {
        // Required empty public constructor
        mXcount = 2;
        mYcount = 4;
        amount = mXcount*mYcount;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSoundBase = new SoundBase(getActivity());
        mCards = mSoundBase.getMemBoard(amount/2);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        PercentFrameLayout frameL = new PercentFrameLayout(getActivity());
        grid = new GridLayout(getActivity());
        grid.setColumnCount(2);
        grid.setOrientation(GridLayout.HORIZONTAL);

        frameL.addView(grid);
        PercentFrameLayout.LayoutParams lp = (PercentFrameLayout.LayoutParams) grid.getLayoutParams();
        PercentLayoutHelper.PercentLayoutInfo li = lp.getPercentLayoutInfo();
        li.aspectRatio = 2;
        lp.height = PercentFrameLayout.LayoutParams.MATCH_PARENT;
        grid.setLayoutParams(lp);

        fillCards(grid);
        return frameL;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void fillCards(GridLayout grid){
        View frontView;
        ImageView backImage;

            Drawable draw = getResources().getDrawable(R.drawable.prahavitta);
            Bitmap bitmap = ((BitmapDrawable)draw).getBitmap();
            int sizeX = bitmap.getWidth()/mXcount;
            int sizeY = bitmap.getHeight()/mYcount;

        int count=0;
        for(int i=0;i<amount;i++){
            PercentFrameLayout fL = (PercentFrameLayout) getActivity().getLayoutInflater().inflate(R.layout.music_card_layout,grid,false);

            frontView = fL.getChildAt(0);
            frontView.setTag(i);
//            Drawable dr = getResources().getDrawable(R.drawable.barney200);
//            button.setBackground(dr);
*/
/*            PercentFrameLayout.LayoutParams lp = (PercentFrameLayout.LayoutParams) button.getLayoutParams();
            PercentLayoutHelper.PercentLayoutInfo lpi = lp.getPercentLayoutInfo();
            lpi.aspectRatio = sizeY/sizeX;
            lp.width = PercentFrameLayout.LayoutParams.MATCH_PARENT;
            button.setLayoutParams(lp);*//*


            backImage = (ImageView)fL.getChildAt(1);
            backImage.setImageBitmap(getPuzzle(bitmap,count,sizeX,sizeY));
            backImage.setScaleType(ImageView.ScaleType.FIT_XY);
            count++;

            grid.addView(fL);
            frontView.setOnTouchListener(new MyTouchListener());
        }
    }

    public Bitmap getPuzzle(Bitmap source,int count, int sizeX, int sizeY) {
        int x = count % mXcount;
        int y = count / mXcount;
        Bitmap result = Bitmap.createBitmap(source,x*sizeX,y*sizeY,sizeX,sizeY);
        return result;
    }

    private class MyTouchListener implements View.OnTouchListener{
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            int  index = (int)view.getTag();
            SoundPuzzle card = mCards.get(index);
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    if ((mCurrentCard!=null)&(mCurrentIndex==-1)) mSoundBase.pause(mCurrentCard);
                    mSoundBase.play(card);
                    if ((mCurrentCard==card)&(mCurrentIndex!=index)) bingo();
                    else {
                        mCurrentCard=card;
                        mCurrentIndex=index;
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mSoundBase.pause(mCurrentCard);
            }
            return true;
        }

        private void bingo(){
            for (int i=0;i<mCards.size();i++){
                if (mCards.get(i).getName()==mCurrentCard.getName()) {
                    View view = grid.findViewWithTag(i);
                    view.setOnTouchListener(null);

                    ViewGroup parent = (ViewGroup) view.getParent();
                    ImageView backView = (ImageView) parent.getChildAt(1);

                    ValueAnimator flipAnimator = ValueAnimator.ofFloat(0f, 1f);
                    int direction = (int)(Math.random()*100)%4;
                    flipAnimator.addUpdateListener(new FlipListener(view, backView,direction));
                    flipAnimator.setDuration(1500);
                    flipAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
                    //flipAnimator.setEvaluator(new FloatEvaluator());
                    flipAnimator.start();
                }
            }
*/
/*            if (bingoCount==0) {
                floatActionButton("replay");
            } else {bingoCount--;}*//*

            mCurrentIndex = -1;
        }

    }
}

*/
