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

    private List<Contact> contacts;
    private MyAdapter myAdapter;
    private Random random;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab1newsfeed, container, false);

        contacts = new ArrayList<>();
        random = new Random();

        //set dummy data
        for (int i = 0; i < 10; i++) {
            Contact contact = new Contact();
            contact.setPhone(phoneNumberGenerating());
            contact.setEmail("DevExchanges" + i + "@gmail.com");
            contacts.add(contact);
        }

        //find view by id and attaching adapter for the RecyclerView
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        myAdapter = new MyAdapter(recyclerView, contacts, getActivity());
        recyclerView.setAdapter(myAdapter);

        //set load more listener for the RecyclerView adapter
        myAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if (contacts.size() <= 200) {
                    contacts.add(null);
                    myAdapter.notifyItemInserted(contacts.size() - 1);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            contacts.remove(contacts.size() - 1);
                            myAdapter.notifyItemRemoved(contacts.size());

                            //Generating more data
                            int index = contacts.size();
                            int end = index + 10;
                            for (int i = index; i < end; i++) {
                                Contact contact = new Contact();
                                contact.setPhone(phoneNumberGenerating());
                                contact.setEmail("DevExchanges" + i + "@gmail.com");
                                contacts.add(contact);
                            }
                            myAdapter.notifyDataSetChanged();
                            myAdapter.setLoaded();
                        }
                    }, 1500);
                } else {
                    Toast.makeText(getActivity(), "Loading data completed", Toast.LENGTH_SHORT).show();
                }
            }
        });


        return rootView;
    }

    private String phoneNumberGenerating() {
        int low = 100000000;
        int high = 999999999;
        int randomNumber = random.nextInt(high - low) + low;

        return "0" + randomNumber;
    }
}
