package com.fantom.vanger.musikcards;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainPager extends AppCompatActivity {
    private ViewPager mainPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_activity);
        mainPager = (ViewPager) findViewById(R.id.pager_layout);
//        mainPager = new ViewPager(this);
//        setContentView(mainPager);
        FragmentManager fManager = getSupportFragmentManager();
        mainPager.setAdapter(new FragmentPagerAdapter(fManager) {
            @Override
            public Fragment getItem(int position) {
                switch(position){
                    case 0:
                        return PuzzleBoardFragment.newInstance(2,2);
                    case 1:
                        return PuzzleBoardFragment.newInstance(2,3);
                    case 2:
                        //return MusicCardsFragment.newInstance();
                    case 3:
                        //return PuzzleBoardFragment.newInstance(3,4);
                }
                return null;
            }

            @Override
            public int getCount() {
                return 4;
            }
        });
        mainPager.setCurrentItem(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.actions,menu);
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menuHint:

                return true;
            case R.id.menuReplay:

                return true;

            case R.id.menuHelp:

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
