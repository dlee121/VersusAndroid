package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.SignUp;

import java.util.ArrayList;

public class GDPRFragment extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private SignUp activity;
    private Toast mToast;

    private TextView mainText, noButton, bottomText;
    private Button yesButton;
    private ScrollView gdprContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.gdpr_consent, container, false);
        gdprContainer = rootView.findViewById(R.id.gdpr_scrollview);
        mainText = rootView.findViewById(R.id.gdpr_main_text);
        String mainString = "Versus personalizes your ad experience using Appodeal. Appodeal and its partners may collect and process personal data such as device identifiers, location data, and other demographic and interest data to provide ads tailored to you. By consenting to this improved ad experience, you’ll see ads that Appodeal and its partners believe are more relevant to you. Learn more.";
        SpannableString mainTextSpannable = new SpannableString(mainString);
        mainTextSpannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.appodeal.com/privacy-policy"));
                startActivity(browserIntent);
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(ContextCompat.getColor(activity, R.color.highlightBlue));

            }
        }, 360, mainString.length()-1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        mainText.setText(mainTextSpannable);
        mainText.setMovementMethod(LinkMovementMethod.getInstance());
        mainText.setHighlightColor(Color.TRANSPARENT);

        noButton = rootView.findViewById(R.id.gdpr_no);
        String noText = "NO, THANK YOU";
        SpannableString noButtonText = new SpannableString(noText);
        noButtonText.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Log.d("gdpraction", "noButton clicked");


            }
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(ContextCompat.getColor(activity, R.color.textBlack));

            }
        }, 0, noText.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        noButton.setText(noButtonText);
        noButton.setMovementMethod(LinkMovementMethod.getInstance());
        noButton.setHighlightColor(Color.TRANSPARENT);


        bottomText = rootView.findViewById(R.id.gdpr_bottom_text);

        yesButton = rootView.findViewById(R.id.gdpr_yes);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("gdpraction", "yesButton clicked");

            }
        });




        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();

        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        disableChildViews();


        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (SignUp) context;
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                enableChildViews();
            }
        }
        else {
            if (rootView != null)
                disableChildViews();
        }
    }


    public void enableChildViews(){
        gdprContainer.setVisibility(View.VISIBLE);
    }

    public void disableChildViews(){
        gdprContainer.setVisibility(View.GONE);
    }

}
