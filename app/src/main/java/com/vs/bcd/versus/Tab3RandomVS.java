package com.vs.bcd.versus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by dlee on 4/29/17.
 */

public class Tab3RandomVS extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab3randomvs, container, false);
        return rootView;
    }

    //TODO: update function intent to launch profile page once profile page is available. For now, it leads to StartScreen.
    public void profileClicked(View view){
        Intent intent = new Intent(getActivity(), WhatsYourBirthday.class);
        //EditText editText = (EditText) findViewById(R.id.editText);
        //String message = editText.getText().toString();
        //intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        getActivity().overridePendingTransition(0, 0);
    }
}