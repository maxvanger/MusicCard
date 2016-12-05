package com.fantom.vanger.musikcards;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.percent.PercentFrameLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PuzzleBoardFragment extends Fragment {
    public static final String TAG="MusicMemFragment";
    private int mXcount;
    private int mYcount;
    private int mAmount = mXcount*mYcount;
    public static final int mPage = 0;
    private Context ctx;
    private Integer[] colors;
    private ArrayList<Integer> colorsList;

    public interface PuzzleBoard {List<Puzzle> getPuzzles(int type);}

    private Presenter mPuzzlePresenter;
    private List<Puzzle> mLocksList;
    private List<Puzzle> mKeysList;
    private View mCurrentKey;
    private Map<Integer,Integer> colorPuzzle = new HashMap<>();
    private int mCurrentIndex;
    private int bingoCount;
    private boolean isDragging;
    /*public static PuzzleBoardFragment newInstance() {
        return new PuzzleBoardFragment(3,3);
    }*/
    public static PuzzleBoardFragment newInstance(int xx, int yy) {

        return new PuzzleBoardFragment(xx,yy);
    }

    public PuzzleBoardFragment(int xx,int yy){
        mXcount=xx;
        mYcount=yy;
        bingoCount = xx*yy;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getActivity();
        //setRetainInstance(true);

        //Get Colors ArrayList from int[] for Collections.shuffle()
        int[] col = ctx.getResources().getIntArray(R.array.anncolor);
        colors = new Integer[col.length];
        for (int i=0;i<col.length;i++){colors[i]=col[i];}
        colorsList = new ArrayList<>(Arrays.asList(colors));

        //Get puzzles: LockList and KeyList
        mPuzzlePresenter = Presenter.newInstance(getActivity(),mXcount,mYcount,mPage);
        mLocksList = mPuzzlePresenter.getPuzzles(mPuzzlePresenter.TYPE_LOCK);
        mKeysList = mPuzzlePresenter.getPuzzles(mPuzzlePresenter.TYPE_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.grids_layout, container, false);
        GridLayout gridLocks = (GridLayout)view.findViewById(R.id.grid_locks);
        gridLocks.setColumnCount(mXcount);
        GridLayout gridKeys = (GridLayout)view.findViewById(R.id.grid_keys);

        Collections.shuffle(colorsList);
        fillCards(gridLocks,R.layout.item_lock_view);
        fillKeys(gridKeys, R.layout.item_lock_view);

        return view;
    }

    private void fillCards(ViewGroup viewGroup,int layoutCard){
        int color,i=0;
        for (Puzzle puzzle: mLocksList){
            color = colorsList.get(i);
            colorPuzzle.put(puzzle.getId(),color);

            FrameLayout fl = (FrameLayout)getActivity().getLayoutInflater().inflate(layoutCard,viewGroup,false);
            //fl.setTag(puzzle.getId());
            //fl.setOnTouchListener(new SoundOnTouchListener());

            View front = fl.getChildAt(0);
            front.setBackground(getShapeDrawable(R.drawable.button_lock_normal,color));
            front.setTag(puzzle.getId());
            front.setTag(R.id.colorTag,Integer.valueOf(color));
            front.setOnTouchListener(new SoundOnTouchListener());
            front.setOnDragListener(new LockDragListener());

            ImageView back = (ImageView) fl.getChildAt(1);
            back.setImageBitmap(puzzle.getImagePuzzle());

            viewGroup.addView(fl);
            i++;
        }
    }

    private void fillKeys(ViewGroup viewGroup, int layoutCard){
        View keyView;
        int color=0,i;
        for (Puzzle puzzle : mKeysList) {
            i = mPuzzlePresenter.getLockId(puzzle.getId());
            if (colorPuzzle.containsKey(i))
                color=colorPuzzle.get(i);
            ViewGroup fl = (ViewGroup)getActivity().getLayoutInflater().inflate(layoutCard,viewGroup,false);
            //fl.setTag(puzzle.getId());
            //fl.setOnTouchListener(new SoundOnTouchListener());

            keyView = fl.getChildAt(0);
            keyView.setBackground(getShapeDrawable(R.drawable.button_key_normal,color));
            keyView.setTag(puzzle.getId());
            keyView.setTag(R.id.colorTag,color);
            keyView.setOnTouchListener(new KeyOnTouchListener());
            //keyView.setOnLongClickListener(new KeyLongClickListener());
            viewGroup.addView(fl);
        }
    }

    protected class SoundOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view,MotionEvent event) {
            int soundId = (int) view.getTag();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mPuzzlePresenter.play(soundId);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mPuzzlePresenter.pause(soundId);
            }
            return true;
        }
    }

    protected class KeyOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view,MotionEvent event) {
            int soundId = (int) view.getTag();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mCurrentKey = view;
                    mPuzzlePresenter.play(soundId);

                    ClipData.Item item = new ClipData.Item((view.getTag()).toString());
                    ClipDescription clipDescription = new ClipDescription(TAG, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
                    ClipData dragData = new ClipData(clipDescription, item);

                    View.DragShadowBuilder myShadow = new MyDragShadowBuilder(view);
                    view.startDrag(dragData, myShadow, null, 0);
                    isDragging = true;
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging) return true;
                case MotionEvent.ACTION_UP:
                    mPuzzlePresenter.pause(soundId);
            }
            return true;
        }
    }

    protected class LockDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            int color = (int)view.getTag(R.id.colorTag);
            int action = dragEvent.getAction();
            //if (lockViewSize==0){lockViewSize = ((Button)view).getWidth();}
            switch(action){
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    view.setBackground(getShapeDrawable(R.drawable.button_lock_over,color));
                    view.invalidate();
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    isDragging = false;
                    if (mCurrentKey!=null){
                        int soundId = (int)mCurrentKey.getTag();
                        mPuzzlePresenter.pause(soundId);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    view.setBackground(getShapeDrawable(R.drawable.button_lock_normal,color));
                    view.invalidate();
                    return true;

                case DragEvent.ACTION_DROP:
                    ClipData.Item item = dragEvent.getClipData().getItemAt(0);
                    Integer dragId = Integer.valueOf(item.getText().toString());
                    if (mPuzzlePresenter.check((Integer)view.getTag(),dragId)){
                        bingo(view);
                    }
                    return true;
                default: break;
            }
            return false;
        }
    }

    private void bingo(View view){
        final FrameLayout parent = (FrameLayout) view.getParent();
        final ImageView backView = (ImageView) parent.getChildAt(1);

//        pa¡rent.setLayerType(View.LAYER_TYPE_HARDWARE,null);
//        backView.setLayerType(View.LAYER_TYPE_HARDWARE,null);

        ValueAnimator flipAnimator = ValueAnimator.ofFloat(0f, 1f);
        int direction = (int)(Math.random()*100)%4;
        flipAnimator.addUpdateListener(new FlipListener(view, backView,direction));
        flipAnimator.setDuration(1400);
        flipAnimator.setInterpolator(new DecelerateInterpolator(1f));
        flipAnimator.setEvaluator(new FloatEvaluator());
/*
        flipAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                parent.setLayerType(View.LAYER_TYPE_NONE,null);
                backView.setLayerType(View.LAYER_TYPE_NONE,null);
            }

        });*/
        flipAnimator.start();
        //searching the keyView with Tag are equil to view.Tag
        mCurrentKey.setVisibility(View.INVISIBLE);
        bingoCount--;
        if(bingoCount==0) {
            bingoCount = mXcount*mYcount;
            Toast.makeText(getActivity(), "Bingo, Maestro!", Toast.LENGTH_SHORT).show();
        }
        //mPuzzlePresenter.play((int)view.getTag());
    }


    protected class KeyLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            mCurrentKey = v;
            ClipData.Item item = new ClipData.Item((v.getTag()).toString());
            ClipDescription clipDescription = new ClipDescription(TAG, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
            ClipData dragData = new ClipData(clipDescription, item);

            View.DragShadowBuilder myShadow = new MyDragShadowBuilder(v);
            v.startDrag(dragData, myShadow, null, 0);
            return true;
        }
    }

    private Drawable getShapeDrawable(int drawable,int color) {
        Drawable dr = getResources().getDrawable(drawable);
        if (dr instanceof ShapeDrawable) {
            ((ShapeDrawable)dr).getPaint().setColor(color);
        } else if (dr instanceof GradientDrawable) {
            ((GradientDrawable)dr).setColor(color);
        }
        return dr;
    }
    private class MyDragShadowBuilder extends View.DragShadowBuilder {

        private Point mScaleFactor;
        public MyDragShadowBuilder(View v) {
            super(v);
        }

        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            int width;
            int height;
            int color = (int)getView().getTag(R.id.colorTag);
            getView().setBackground(getShapeDrawable(R.drawable.button_key_over,color));
            width = (int)Math.round(getView().getWidth() * 1.7);
            height = (int)Math.round(getView().getHeight() * 1.7);
            // Sets the size parameter's width and height values. These get back to the system
            // through the size parameter.
            size.set(width, height);

            mScaleFactor = size;

            // Sets the touch point's position to be in the middle of the drag shadow
            // Sets size parameter to member that will be used for scaling shadow image.
            touch.set(width/2,height/2);
            Log.d(TAG, "onProvideShadowMetrics: "+getView().getWidth());
        }

        @Override
        public void onDrawShadow(Canvas canvas) {

            // Draws the ColorDrawable in the Canvas passed in from the system.
            canvas.scale(mScaleFactor.x/(float)getView().getWidth(), mScaleFactor.y/(float)getView().getHeight());
            getView().draw(canvas);
        }

    }
}
