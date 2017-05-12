package com.vs.bcd.versus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.os.Handler;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by dlee on 4/29/17.
 */

public class Tab1Newsfeed extends Fragment{

    private List<Post> posts;
    private MyAdapter myAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab1newsfeed, container, false);
        //TODO: need to add categories. maybe a separate categories table where post IDs have rows of categories they are linked with
        //TODO: this is where I need to get data from database and construct list of Post objects
        //TODO: create, at the right location, list of constant enumeration to represent categories. probably at post creation page, which is for now replaced by sample data creation below
        //for now, create sample data
        posts = new ArrayList<>();

        Post tempPost;

        //sample post 1
        //categories: for now let's do 0 = politics, 1 = sports (then sub categories / tags could be basket ball, boxing, ufc, soccer, etc), 2 = food, 3 = anime / comics
        //set up enum of Strings to prevent errors like typos
        tempPost = new Post();
        tempPost.setPostID(0xABCD12);
        tempPost.setQuestion("Who would win in a fight?");
        tempPost.setAuthor("bingbing123");
        tempPost.setTime("5 days");
        tempPost.setViewcount(84133);
        tempPost.setRedname("Conor McGregor");
        tempPost.setRedcount(42214);
        tempPost.setBlackname("Floyd Mayweather Jr.");
        tempPost.setBlackcount(38139);
        tempPost.setCategory("sports");

        posts.add(tempPost);

        //sample post 2
        tempPost = new Post();
        tempPost.setPostID(0xBBC113);
        tempPost.setQuestion("Should heroin be legalized?");
        tempPost.setAuthor("mingming22");
        tempPost.setTime("5 minutes");
        tempPost.setViewcount(3133);
        tempPost.setRedname("Yes");
        tempPost.setRedcount(74);
        tempPost.setBlackname("No");
        tempPost.setBlackcount(239);
        tempPost.setCategory("politics");
        posts.add(tempPost);

        //sample post 3
        tempPost = new Post();
        tempPost.setPostID(0xB8C713);
        tempPost.setQuestion("Which one's better?");
        tempPost.setAuthor("mingming22");
        tempPost.setTime("3 months");
        tempPost.setViewcount(412423);
        tempPost.setRedname("Burger");
        tempPost.setRedcount(8812);
        tempPost.setBlackname("Pizza");
        tempPost.setBlackcount(9345);
        tempPost.setCategory("food");
        posts.add(tempPost);

        //sample post 4
        tempPost = new Post();
        tempPost.setPostID(0xA23614);
        tempPost.setQuestion("Who would win in a fight?");
        tempPost.setAuthor("12pongpong48");
        tempPost.setTime("8/10/2016");
        tempPost.setViewcount(932983);
        tempPost.setRedname("Goku");
        tempPost.setRedcount(9001);
        tempPost.setBlackname("Superman");
        tempPost.setBlackcount(8639);
        tempPost.setCategory("anime / comics");
        posts.add(tempPost);


        //find view by id and attaching adapter for the RecyclerView
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        //this is where the list is passed on to adapter
        myAdapter = new MyAdapter(recyclerView, posts, getActivity());
        recyclerView.setAdapter(myAdapter);

        //set load more listener for the RecyclerView adapter
        myAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {

                if (posts.size() <= 3) {
                    /*
                    posts.add(null);
                    myAdapter.notifyItemInserted(posts.size() - 1);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            posts.remove(posts.size() - 1);
                            myAdapter.notifyItemRemoved(posts.size());

                            //Generating more data
                            int index = posts.size();
                            int end = index + 10;
                            for (int i = index; i < end; i++) {
                                Contact contact = new Contact();
                                contact.setEmail("DevExchanges" + i + "@gmail.com");
                                posts.add(contact);
                            }
                            myAdapter.notifyDataSetChanged();
                            myAdapter.setLoaded();
                        }
                    }, 1500);
                    */
                } else {
                    Toast.makeText(getActivity(), "Loading data completed", Toast.LENGTH_SHORT).show();
                }

            }
        });


        return rootView;
    }

}
