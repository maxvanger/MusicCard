package com.fantom.vanger.musikcards;

import android.animation.ValueAnimator;
import android.view.View;

/**
 * downloaded by vanger on 29/09/16.
 * http://stackoverflow.com/questions/7785649/creating-a-3d-flip-animation-in-android-using-xml/8109197#8109197
 */
public class FlipListener implements ValueAnimator.AnimatorUpdateListener {
    public static final int left2right=0;
    public static final int right2left=1;
    public static final int top2bottom=2;
    public static final int bottom2top=3;

    private final View mFrontView;
    private final View mBackView;
    private boolean mFlipped;
    private int mDirection;

    public FlipListener(final View front, final View back, int direction) {
        this.mFrontView = front;
        this.mBackView = back;
        this.mBackView.setVisibility(View.GONE);
        mDirection = direction;
    }

    @Override
    public void onAnimationUpdate(final ValueAnimator animation) {
        final float value = animation.getAnimatedFraction();
        final float scaleValue = 0.625f + (1.5f * (value - 0.5f) * (value - 0.5f));

        if(value < 0.5f){
            rotationFront(value);
            this.mFrontView.setScaleX(scaleValue);
            this.mFrontView.setScaleY(scaleValue);
            if(mFlipped){
                setStateFlipped(false);
            }
        } else {
            rotationBack(value);
            this.mBackView.setScaleX(scaleValue);
            this.mBackView.setScaleY(scaleValue);
            if(!mFlipped){
                setStateFlipped(true);
            }
        }
    }

    private void rotationFront(float value) {
        switch(mDirection){
            case left2right:
                this.mFrontView.setRotationX(180*value);break;
            case right2left:
                this.mFrontView.setRotationX(-180*value);break;
            case top2bottom:
                this.mFrontView.setRotationY(180*value);break;
            case bottom2top:
                this.mFrontView.setRotationY(-180*value);break;
        }
    }

    private void rotationBack(float value){
        switch(mDirection){
            case left2right:
                this.mBackView.setRotationX(180*(value-1f));break;
            case right2left:
                this.mBackView.setRotationX(180*(1f-value));break;
            case top2bottom:
                this.mBackView.setRotationY(180*(value-1f));break;
            case bottom2top:
                this.mBackView.setRotationY(180*(1f-value));break;
        }
    }

    private void setStateFlipped(boolean flipped) {
        mFlipped = flipped;
        this.mFrontView.setVisibility(flipped ? View.GONE : View.VISIBLE);
        this.mBackView.setVisibility(flipped ? View.VISIBLE : View.GONE);
    }
}
