package com.vs.bcd.versus.model;

import android.util.Log;

import com.vs.bcd.versus.fragment.PostPage;
import com.vs.bcd.versus.fragment.Tab1Newsfeed;
import com.vs.bcd.versus.fragment.Tab2Trending;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dlee on 8/21/17.
 */

public class ThreadCounter {

    private AtomicInteger n;
    private int limit;
    private int tabNumber = 0;
    private Tab1Newsfeed tab1;
    private Tab2Trending tab2;
    private PostPage postPage;

    public ThreadCounter (int n, int limit, Tab1Newsfeed tab1){
        this.n = new AtomicInteger(n);
        this.limit = limit;
        this.tab1 = tab1;
        tabNumber = 1;
    }

    public ThreadCounter (int n, int limit, Tab2Trending tab2){
        this.n = new AtomicInteger(n);
        this.limit = limit;
        this.tab2 = tab2;
        tabNumber = 2;
    }

    public ThreadCounter (int n, int limit, PostPage postPage){
        this.n = new AtomicInteger(n);
        this.limit = limit;
        this.postPage = postPage;
        tabNumber = 3;
    }

    public void increment(){
        if(n.incrementAndGet() == limit){
            switch (tabNumber){
                case 1:
                    tab1.yesDisplayResults();
                    break;
                case 2:
                    tab2.yesDisplayResults();
                    break;
                case 3:
                    postPage.yesExitLoop();
                    break;
                default:
                    break;
            }
        }
    }

    public int getVal(){
        return n.get();
    }
}
