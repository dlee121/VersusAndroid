package com.vs.bcd.versus.activity;

import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.fragment.Tab1Newsfeed;
import com.vs.bcd.versus.fragment.Tab2Trending;
import com.vs.bcd.versus.fragment.Tab3Categories;
import com.vs.bcd.versus.fragment.Tab4Messenger;

import java.util.ArrayList;

public class MainActivity extends Fragment {
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentStatePagerAdapter} derivative
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private View rootView;
    private boolean childrenFragmentsUIActive = false;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private RelativeLayout.LayoutParams fabLP;
    private LinearLayout tabStrip;
    private MainContainer mainContainer;
    private FloatingActionButton fab;
    private Tab4Messenger tab4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_main, container, false);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) rootView.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);

        mainContainer = (MainContainer)getActivity();

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabStrip = ((LinearLayout)tabLayout.getChildAt(0));
        //set tab icons

        tabLayout.getTabAt(0).setIcon(R.drawable.ic_trending_selected);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_trending_unselected);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_trending_unselected);
        tabLayout.getTabAt(3).setIcon(R.drawable.ic_messenger_unselected_24dp);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: //newsfeed
                        tab.setIcon(R.drawable.ic_trending_selected);
                        mainContainer.setToolbarTitleTextForTabs("Newsfeed");
                        enableCPFab();
                        break;
                    case 1: //trending
                        tab.setIcon(R.drawable.ic_trending_selected);
                        mainContainer.setToolbarTitleTextForTabs("Trending");
                        enableCPFab();
                        break;
                    case 2: //categories
                        tab.setIcon(R.drawable.ic_trending_selected);
                        mainContainer.setToolbarTitleTextForTabs("Categories");
                        disableCPFab();
                        break;
                    case 3: //messenger
                        tab.setIcon(R.drawable.ic_messenger_selected_24dp);
                        mainContainer.setToolbarTitleTextForTabs("Messenger");
                        disableCPFab();
                        break;
                    default:
                        tab.setIcon(R.drawable.ic_trending_selected);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: //newsfeed
                        tab.setIcon(R.drawable.ic_trending_unselected);
                        break;
                    case 1: //trending
                        tab.setIcon(R.drawable.ic_trending_unselected);
                        break;
                    case 2: //categories
                        tab.setIcon(R.drawable.ic_trending_unselected);
                        break;
                    case 3: //messenger
                        tab.setIcon(R.drawable.ic_messenger_unselected_24dp);
                        break;
                    default:
                        tab.setIcon(R.drawable.ic_trending_unselected);
                        break;
                }

            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fabLP = (RelativeLayout.LayoutParams) fab.getLayoutParams();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainContainer.getCreatePostFragment().resetCatSelection();
                mainContainer.getViewPager().setCurrentItem(2);
                mainContainer.getToolbarTitleText().setText("Create Post");
                mainContainer.getToolbarButtonLeft().setImageResource(R.drawable.ic_left_chevron);
            }
        });

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        return rootView;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
    /*
        //Search_Main button was pressed
        if (id == R.id.action_search_main) {
            //TODO:Activate Search UI and Implement Main Search function
            return true;
        }
    */
        return super.onOptionsItemSelected(item);
    }

    //deleted PlaceholderFragment class
    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            //Return current tabs
            switch (position) {
                case 0:
                    Tab1Newsfeed tab1 = new Tab1Newsfeed();
                    return tab1;
                case 1:
                    Tab2Trending tab2 = new Tab2Trending();
                    return tab2;
                case 2:
                    Tab3Categories tab3 = new Tab3Categories();
                    return tab3;
                case 3:
                    tab4 = new Tab4Messenger();
                    return tab4;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 4;
        }
    }

    public ViewPager getViewPager(){
        return mViewPager;
    }


    public boolean getUILifeStatus(){
        return childrenFragmentsUIActive;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            childrenFragmentsUIActive = true;
            if(rootView != null){
                enableChildViews();
                int currItem = mViewPager.getCurrentItem();
                if(currItem == 2 || currItem == 3){
                    disableCPFab();
                }
            }
        }
        else{
            childrenFragmentsUIActive = false;
            if (rootView != null)
                disableChildViews();
        }
    }

    public void enableChildViews(){
        /* commented these out since resetCatSelection handles these operations now
        redimgSet = "default";
        blackimgSet = "default";
        */
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);
            childViews.get(i).setLayoutParams(LPStore.get(i));

        }
    }

    public void disableChildViews(){
        Log.d("disabling", "This many: " + Integer.toString(childViews.size()));
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }

    private void enableCPFab(){
        fab.setEnabled(true);
        fab.setClickable(true);
        fab.setLayoutParams(fabLP);
    }

    private void disableCPFab(){
        fab.setEnabled(false);
        fab.setClickable(false);
        fab.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
    }

    public Tab4Messenger getTab4Messenger(){
        return tab4;
    }

}