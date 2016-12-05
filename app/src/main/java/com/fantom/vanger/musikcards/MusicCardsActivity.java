package com.fantom.vanger.musikcards;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.percent.PercentFrameLayout;
import android.support.percent.PercentLayoutHelper;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MusicCardsActivity extends AppCompatActivity implements MixPresenterStore.PuzzleListLoader {
    public static final int COUNT_X = 2;
    public static final int COUNT_Y = 4;

    private int mXcount;
    private int mYcount;
    private int countBingo;
    private int countAttempts;
    private int mKindOfCards;
    private MixPresenterStore mpStore;
    private GridLayout grid;
    private List<Puzzle> puzzleList;
    private Puzzle mCurrentPuzzle;
    private int mCurrentIndex, mPairPuzzleViewId;
    private View pairView;
    private int mLevelCount;
    private boolean isHint;
    private boolean isSecondOfTwo;
    private boolean isUserChoice;
    private SharedPreferences settings;
    private List<String> audioList = new ArrayList<>();
    private int frontDrawableId;
    private final int[] backgroundId = {R.drawable.background_1,R.drawable.background_2, R.drawable.background_3};
    private int gamesCount=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_cards);

        settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        mXcount = settings.getInt(SettingsActivity.STRING_COLUMN, COUNT_X);
        mYcount = settings.getInt(SettingsActivity.STRING_ROW, COUNT_Y);
        mKindOfCards = settings.getInt(SettingsActivity.KIND_CARD, R.id.set_same);
        mLevelCount = settings.getInt(SettingsActivity.LEVEL_COUNT, 1);
        //isUserChoice = settings.getBoolean(USER_CHOICE,false);

        grid = (GridLayout) findViewById(R.id.grid_space);
        grid.setColumnCount(mXcount);

        //may be mpStore - singleton?
        mpStore = new MixPresenterStore(this, mXcount, mYcount, mKindOfCards);
        boolean assetsToDb = settings.getBoolean(SettingsActivity.ASSETS_DB,true);
        if (assetsToDb) {
            settings.edit().putBoolean(SettingsActivity.ASSETS_DB,false).commit();
            mpStore.assetsInsertToProvider();
        }
        mpStore.loadPuzzles(mLevelCount, mKindOfCards);

        frontDrawableId = R.drawable.q_empty;
    }

    @Override
    public void onLoadPuzzleList(List<Puzzle> puzzleList) {
        this.puzzleList = puzzleList;
        createGameBoard();
    }

    private void createGameBoard() {
        countBingo = mXcount * mYcount / 2;
        countAttempts = 0;
        isHint = false;
        isSecondOfTwo = true;
        View frontView;
        ImageView backView;
        ViewGroup viewGroup;
        findViewById(R.id.fab).setVisibility(View.INVISIBLE);
        gamesCount++;
        Drawable drw = getResources().getDrawable(backgroundId[gamesCount%backgroundId.length]);
        findViewById(R.id.percent_fl).setBackground(drw);

        PercentFrameLayout.LayoutParams lp = (PercentFrameLayout.LayoutParams) grid.getLayoutParams();
        PercentLayoutHelper.PercentLayoutInfo li = lp.getPercentLayoutInfo();

        //TODO WTF are happening here at sometimes (Null pointer Excep at ImageBitmap)
        // mpStore are new, BUT ImageBitmap - from past...
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float scrRatio = (float) dm.widthPixels / (float) dm.heightPixels;
        float ratio = mpStore.getImageRatio();
        if (ratio < 1) { //what about WIDTH of PercentFrameLayout?!
            li.heightPercent = 0.9f;
            li.widthPercent = GridLayout.UNDEFINED;
            lp.height = PercentFrameLayout.LayoutParams.MATCH_PARENT;
//            lp.width = GridLayout.UNDEFINED;
        } else {
            li.widthPercent = 0.9f;
            li.heightPercent = GridLayout.UNDEFINED;
            lp.width = PercentFrameLayout.LayoutParams.MATCH_PARENT;
//            lp.height = GridLayout.UNDEFINED;
        }
        li.aspectRatio = ratio;
        grid.setLayoutParams(lp);
        grid.setColumnCount(mXcount);
        grid.requestLayout();

        for (Puzzle puzzle : puzzleList) {
            viewGroup = (ViewGroup) getLayoutInflater().inflate(R.layout.item_lock_view, (ViewGroup) grid, false);

            frontView = viewGroup.getChildAt(0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                frontView.setBackground(getResources().getDrawable(frontDrawableId));
            } else
                frontView.setBackgroundDrawable(getResources().getDrawable(frontDrawableId));
            frontView.setTag(puzzle);
            frontView.setOnTouchListener(new puzzleOnTouchListener());

            backView = (ImageView) viewGroup.getChildAt(1);
            backView.setImageBitmap(puzzle.getImagePuzzle());

            grid.addView(viewGroup);
        }
    }

    private class puzzleOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            Puzzle puzzle = (Puzzle) view.getTag();
            int puzzleId = puzzle.getId();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:

                    if (puzzle != mCurrentPuzzle) {
                        isSecondOfTwo = !isSecondOfTwo;
                    }
                    if ((mCurrentPuzzle != null) && (mCurrentIndex == -1)) {
                        mpStore.releaseMP(mCurrentPuzzle.getId());
                    }

                    view.setAlpha(0.4f);
                    mpStore.play(puzzleId);

                    boolean check = false;
                    switch (mKindOfCards) {
                        case R.id.set_diff:
                            //check = mpStore.check(puzzleId, mCurrentIndex) || mpStore.check(mCurrentIndex, puzzleId);
                            if (mCurrentPuzzle != null) {
                                check = (puzzle.getLinkId() == mCurrentPuzzle.getId()) || (puzzle.getId() == mCurrentPuzzle.getLinkId());
                            }
                            break;
                        case R.id.set_same:
                        case R.id.set_own:
                            check = (puzzle != mCurrentPuzzle) && (puzzleId == mCurrentIndex);
                    }

                    if (check && isSecondOfTwo) bingo(puzzle);
                    else {
                        if (mCurrentPuzzle != null) {
                            ViewGroup relativeL = (ViewGroup) view.getParent();
                            pairView = ((ViewGroup) relativeL.getParent()).findViewWithTag(mCurrentPuzzle);
//                            mPairPuzzleViewId = pairView.getId();
//                            pairView.setAlpha(1f);
                        }
                        mCurrentPuzzle = puzzle;
                        mCurrentIndex = puzzleId;
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mpStore.pause(puzzleId);

                    if (isSecondOfTwo) {
                        view.setAlpha(1f);
                        pairView.setAlpha(1f);
                        countAttempts++;
                    }
            }
            return true;
        }
    }

    private void bingo(final Puzzle puzzle) {
        //puzzle - it's last touched and playing puzzle, current now!
        //mCurrentPuzzle - it's a couple for puzzle, not current now!
        ViewGroup parent;
        ImageView backView;
        Puzzle[] couple = {puzzle, mCurrentPuzzle};

        if (puzzle.getId() != mCurrentIndex) mpStore.releaseMP(mCurrentIndex);

        for (Puzzle pzl : couple) {
            View view = grid.findViewWithTag(pzl);
            view.setOnTouchListener(null);

            parent = (ViewGroup) view.getParent();
            backView = (ImageView) parent.getChildAt(1);

            ValueAnimator flipAnimator = ValueAnimator.ofFloat(0f, 1f);
            int direction = (int) (Math.random() * 100) % 4;
            flipAnimator.addUpdateListener(new FlipListener(view, backView, direction));
            flipAnimator.setDuration(1500);
            flipAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            //flipAnimator.setEvaluator(new FloatEvaluator());
            flipAnimator.start();

            Toast.makeText(this, puzzle.getTitle(), Toast.LENGTH_SHORT).show();
        }
        mCurrentPuzzle = puzzle;
        mCurrentIndex = -1;
        countBingo--;
        if (countBingo == 0) {
            mLevelCount++;
            if (mLevelCount > 2) mLevelCount = 1; //stub for only two exist levels

            //TODO show Dialog with Scores:
            // Attempts: #countAttempts = countAttempts + Bingos count = 12 + 6 (etc..)
            // Score: (mAmount/2)*100/countAttempts = 60 (etc..
            // Total: total + Score(60)
            // Button: next/repeat (kick FAB)
            // save Score's result to SharedPref

            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(SettingsActivity.LEVEL_COUNT, mLevelCount);
            editor.commit();

            FragmentManager fm = getSupportFragmentManager();
            int totalPair = mXcount*mYcount/2;
            countAttempts+=totalPair;
            DialogScore dialogScore = DialogScore.newInstace(countAttempts,totalPair);
            dialogScore.show(fm,"DIALOG SCORE");

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mpStore.releaseMP(puzzle.getId());
                    mCurrentIndex = 0;
                    mCurrentPuzzle = null;
                    grid.removeAllViews();
                    // create new level (new sounds, next album)
                    mpStore.reset();
                    mpStore.loadPuzzles(mLevelCount, mKindOfCards);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuHint:
                isHint = !isHint;
                showHint(isHint);
                return true;
            case R.id.menuReplay:
                if (mCurrentPuzzle != null) mpStore.releaseMP(mCurrentPuzzle.getId());
                mpStore.reset();
                mpStore.setPuzzledImage();

                mCurrentPuzzle = null;
                grid.removeAllViews();
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
        if (mCurrentPuzzle != null) {
            mpStore.pause(mCurrentPuzzle.getId());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCurrentPuzzle != null) {
            mpStore.releaseMP(mCurrentPuzzle.getId());
        }
    }

    private void showHint(boolean flag) {
        ViewGroup vg;
        Puzzle p;
        Drawable drw;
        for (int i = 0; i < grid.getChildCount(); i++) {
            vg = (ViewGroup) grid.getChildAt(i);
            View frontView = vg.getChildAt(0);
            p = (Puzzle) frontView.getTag();

            if (flag) drw = p.getImageHint();
            else drw = getResources().getDrawable(frontDrawableId);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                frontView.setBackground(drw);
            } else frontView.setBackgroundDrawable(drw);
        }
    }

    private View generateView() {
        PercentFrameLayout layout = new PercentFrameLayout(this);
        grid = new GridLayout(this);
        grid.setColumnCount(mXcount);
        grid.setOrientation(GridLayout.HORIZONTAL);

        layout.addView(grid);
        PercentFrameLayout.LayoutParams lp = (PercentFrameLayout.LayoutParams) grid.getLayoutParams();
        PercentLayoutHelper.PercentLayoutInfo li = lp.getPercentLayoutInfo();
        li.aspectRatio = 2;
        lp.height = PercentFrameLayout.LayoutParams.MATCH_PARENT;
        grid.setLayoutParams(lp);

        fillCards(grid);
        return layout;
    }

    private void fillCards(GridLayout grid) {
    }


}
