package com.fantom.vanger.musikcards;

import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.percent.PercentFrameLayout;
import android.support.percent.PercentLayoutHelper;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fantom.vanger.musikcards.SettingsActivity.ASSETS_DB;

public class MusicPairsActivity extends AppCompatActivity implements MixPresenterStore.PuzzleListLoader {
    public static final String PREFS_NAME="settingsFile";
    public static final String STRING_COLUMN ="columns";
    public static final String STRING_ROW ="rows";
    public static final String KIND_CARD="kindCard";
    public static final String LEVEL_COUNT="levelCount";
    public static final int COUNT_X = 2;
    public static final int COUNT_Y = 4;
    private static final String TAG = "MusicPairsActivity";

    private int mXcount;
    private int mYcount;
    private int countBingo;
    private int countAttempts;
    private int mKindOfCards;
    private MixPresenterStore mpStore;
    private GridLayout gridLocks,gridKeys;
    private List<Puzzle> puzzleList;
    private Puzzle mCurrentPuzzle;
    private int mCurrentIndex;
    private View mCurrentKey;
    private int mLevelCount;
    private boolean isHint;
    private boolean isDragging;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grids_layout);

        settings = getSharedPreferences(PREFS_NAME,0);
        mXcount = settings.getInt(STRING_COLUMN,COUNT_X);
        mYcount = settings.getInt(STRING_ROW,COUNT_Y);
        mLevelCount = settings.getInt(LEVEL_COUNT,1);

        //mKindOfCards = settings.getInt(KIND_CARD,R.id.set_diff);
        mKindOfCards = MixPresenterStore.TYPE_LOCK_KEY;

        gridLocks = (GridLayout) findViewById(R.id.grid_locks);
        gridLocks.setColumnCount(mXcount);
        gridKeys = (GridLayout) findViewById(R.id.grid_keys);

        //may be mpStore - singleton?
        mpStore = new MixPresenterStore(this, mXcount, mYcount, mKindOfCards);
        boolean assetsToDb = settings.getBoolean(ASSETS_DB,true);
        if (assetsToDb) {
            settings.edit().putBoolean(ASSETS_DB,false).commit();
            mpStore.assetsInsertToProvider();
        }
        mpStore.loadPuzzles(mLevelCount, mKindOfCards);
        mpStore.setColorsHints(getColorsList());
    }

    private List<Integer> getColorsList(){
        //Get Colors ArrayList from int[] for Collections.shuffle()
        int[] col = this.getResources().getIntArray(R.array.anncolor);
        Integer[] colors = new Integer[col.length];
        for (int i=0;i<col.length;i++){colors[i]=col[i];}
        List<Integer> colorsList = new ArrayList<>(Arrays.asList(colors));
        return colorsList;
    }

    @Override
    public void onLoadPuzzleList(List<Puzzle> puzzleList) {
        this.puzzleList = puzzleList;
        createGameBoard();
    }

    private void createGameBoard() {
        countBingo = mXcount*mYcount;
        countAttempts = 0;
        isHint = false;
        findViewById(R.id.fab_pair).setVisibility(View.INVISIBLE);

        PercentFrameLayout.LayoutParams lp = (PercentFrameLayout.LayoutParams) gridLocks.getLayoutParams();
        PercentLayoutHelper.PercentLayoutInfo li = lp.getPercentLayoutInfo();
        //TODO WTF are happening here at sometimes (Null pointer Excep at ImageBitmap)
        // mpStore are new, BUT ImageBitmap - from past...
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float scrRatio = (float)dm.widthPixels/(float)dm.heightPixels;
        float ratio= mpStore.getImageRatio();
        li.aspectRatio = ratio;
        if (ratio<1) { //what about WIDTH of PercentFrameLayout?!
            li.heightPercent=0.98f;
            li.widthPercent=GridLayout.UNDEFINED;
        } else {
            li.widthPercent=0.98f;
            li.heightPercent=GridLayout.UNDEFINED;
        }
        li.aspectRatio = ratio;
        gridLocks.setLayoutParams(lp);
        gridLocks.setColumnCount(mXcount);
        gridLocks.requestLayout();

        fillCards();
    }

    private void fillCards(){
        View frontView;
        ImageView backView;
        ViewGroup viewGroup,grid;

        for(Puzzle puzzle : puzzleList){

            if (puzzle.getLinkId()==-1) grid = gridLocks;
            else grid = gridKeys;

            viewGroup = (ViewGroup) getLayoutInflater().inflate(R.layout.item_lock_view,grid,false);

            frontView = viewGroup.getChildAt(0);
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
                frontView.setBackground(getResources().getDrawable(R.drawable.scorepaper0));
            } else frontView.setBackgroundDrawable(getResources().getDrawable(R.drawable.scorepaper0));
            frontView.setTag(puzzle);

            backView = (ImageView) viewGroup.getChildAt(1);
            backView.setImageBitmap(puzzle.getImagePuzzle());

            if (puzzle.getLinkId()==-1) {
                //Lock Puzzles set listener
                frontView.setOnTouchListener(new LockOnTouchListener());
                frontView.setOnDragListener(new LockOnDragListener());
            } else {
                //Key puzzles set listener
                frontView.setOnTouchListener(new KeyOnTouchListener());
            }

            grid.addView(viewGroup);
        }

    }

    protected class KeyOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view,MotionEvent event) {
            Puzzle puzzle= (Puzzle)view.getTag();
            int soundId = puzzle.getId();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (mCurrentKey!=null) mpStore.pause(((Puzzle)mCurrentKey.getTag()).getId());

                    mCurrentKey = view;
                    mpStore.play(soundId);

                    ClipData.Item item = new ClipData.Item(Integer.toString(puzzle.getLinkId()));
                    ClipDescription clipDescription = new ClipDescription(TAG, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN});
                    ClipData dragData = new ClipData(clipDescription, item);

                    View.DragShadowBuilder myShadow = new MyDragShadowBuilder(view);
                    view.startDrag(dragData, myShadow, null, 0);
                    isDragging = true;
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging) return true;
                case MotionEvent.ACTION_UP:
                    mpStore.pause(soundId);
            }
            return true;
        }
    }

    protected class LockOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view,MotionEvent event) {
            int soundId = ((Puzzle) view.getTag()).getId();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (mCurrentKey!=null) mpStore.pause(((Puzzle)mCurrentKey.getTag()).getId());
                    mpStore.play(soundId);
                    view.setAlpha(0.6f);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mpStore.pause(soundId);
                    view.setAlpha(1.0f);
            }
            return true;
        }
    }

    protected class LockOnDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            Puzzle puzzle = (Puzzle)view.getTag();
            int soundId = 0;
            if (mCurrentKey!=null) soundId = ((Puzzle)mCurrentKey.getTag()).getId();

            int action = dragEvent.getAction();
            switch(action){
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
//                    view.setBackground(getShapeDrawable(R.drawable.button_lock_over,color));
//                    view.invalidate();
                    view.setAlpha(0.6f);
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    view.setAlpha(1.0f);
                    isDragging = false;
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
//                    view.setBackground(getShapeDrawable(R.drawable.button_lock_normal,color));
//                    view.invalidate();
                    view.setAlpha(1.0f);
                    isDragging = false;
                    return true;

                case DragEvent.ACTION_DROP:
                    countAttempts++;
                    ClipData.Item item = dragEvent.getClipData().getItemAt(0);
                    Integer dragId = Integer.valueOf(item.getText().toString());
                    if (puzzle.getId()==dragId) bingo(view);
//                    else if (mCurrentKey!=null) mpStore.pause(soundId);

                    return true;
                default: break;
            }
            return false;
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
//            int color = (int)getView().getTag(R.id.colorTag);
//            getView().setBackground(getShapeDrawable(R.drawable.button_key_over,color));
//            Drawable drw = ((Puzzle)getView().getTag()).getImageHint();
//            getView().setBackgroundDrawable(drw);
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

    private void bingo(View view){
        final FrameLayout parent = (FrameLayout) view.getParent();
        final ImageView backView = (ImageView) parent.getChildAt(1);

        Toast.makeText(this, ((Puzzle)mCurrentKey.getTag()).getTitle(), Toast.LENGTH_SHORT).show();

        ValueAnimator flipAnimator = ValueAnimator.ofFloat(0f, 1f);
        int direction = (int)(Math.random()*100)%4;
        flipAnimator.addUpdateListener(new FlipListener(view, backView,direction));
        flipAnimator.setDuration(1400);
        flipAnimator.setInterpolator(new DecelerateInterpolator(1f));
        flipAnimator.setEvaluator(new FloatEvaluator());
        flipAnimator.start();
        mCurrentKey.setVisibility(View.INVISIBLE);

        countBingo--;
        if(countBingo==0) {
            Toast.makeText(this, "bingo Maestro!", Toast.LENGTH_SHORT).show();
            mLevelCount++;
            if (mLevelCount>2) mLevelCount=1; //stub for only two exist levels

            //TODO show Dialog with Scores:
            // Attempts: #countAttempts = countAttempts + Bingos count = 12 + 6 (etc..)
            // Score: (mAmount/2)*100/countAttempts = 60 (etc..
            // Total: total + Score(60)
            // Button: next/repeat (kick FAB)
            // save Score's result to SharedPref

            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(LEVEL_COUNT,mLevelCount);
            editor.commit();

            FragmentManager fm = getSupportFragmentManager();
            DialogScore dialogScore = DialogScore.newInstace(countAttempts,mXcount*mYcount);
            dialogScore.show(fm,"DIALOG SCORE");

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_pair);
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gridLocks.removeAllViews();
                    gridKeys.removeAllViews();
                    // create new level (new sounds, next album)
                    mpStore.reset();
                    mpStore.loadPuzzles(mLevelCount,mKindOfCards);
                }
            });

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menuHint:
                isHint=!isHint;
                showHint(isHint);
                return true;
            case R.id.menuReplay:
                if (mCurrentPuzzle!=null) mpStore.releaseMP(mCurrentPuzzle.getId());
                mpStore.reset();
                mpStore.setPuzzledImage();

                mCurrentPuzzle = null;
                gridLocks.removeAllViews();
                gridKeys.removeAllViews();
                createGameBoard();
                return true;

            case R.id.menuHelp:

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCurrentKey!=null) mpStore.pause(((Puzzle)mCurrentKey.getTag()).getId());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCurrentKey!=null) mpStore.pause(((Puzzle)mCurrentKey.getTag()).getId());
    }

    private void showHint(boolean flag) {
        for(int i=0;i<gridLocks.getChildCount();i++){
            View frontLock = ((ViewGroup) gridLocks.getChildAt(i)).getChildAt(0);
            View frontKey = ((ViewGroup) gridKeys.getChildAt(i)).getChildAt(0);
            showHintView(frontLock,flag);
            showHintView(frontKey,flag);
        }
    }
    private void showHintView(View view,boolean flag){
        Drawable drw;
        Puzzle p = (Puzzle) view.getTag();

        if (flag) drw = p.getImageHint();
        else drw = getResources().getDrawable(R.drawable.scorepaper0);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drw);
        } else view.setBackgroundDrawable(drw);

    }
}
