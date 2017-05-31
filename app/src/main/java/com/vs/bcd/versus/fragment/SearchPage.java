package com.vs.bcd.versus.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vs.bcd.versus.R;

/**
 * Created by dlee on 5/19/17.
 */

public class SearchPage extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.search_page, container, false);
        return rootView;
    }

}
