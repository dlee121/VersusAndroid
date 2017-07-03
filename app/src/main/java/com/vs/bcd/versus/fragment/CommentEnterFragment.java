package com.vs.bcd.versus.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.PostPageAdapter;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.VSComment;

import org.w3c.dom.Text;

import java.util.ArrayList;

import static android.R.attr.left;
import static android.R.attr.onClick;
import static android.R.attr.right;

/**
 * Created by dlee on 7/1/17.
 */

public class CommentEnterFragment extends Fragment{
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private TextView questionTV, vsX, vsY;
    private Button submitButton;
    private String postID = "";
    private Post post;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.comment_enter_fragment , container, false);

        questionTV = (TextView)rootView.findViewById(R.id.postquestion);
        vsX = (TextView)rootView.findViewById(R.id.vsx);
        vsY = (TextView)rootView.findViewById(R.id.vsy);
        submitButton = (Button)rootView.findViewById(R.id.submitButton);

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: form validation (although most strings should be acceptable as comments anyway)
                Runnable runnable = new Runnable() {
                    public void run() {
                        VSComment vsc = new VSComment();
                        vsc.setPost_id(post.getPost_id());
                        vsc.setParent_id("0");  //TODO: for root/reply check, which would be more efficient, checking if parent_id == "0" or checking parent_id.length() == 1?
                        vsc.setAuthor(((MainContainer)getActivity()).getSessionManager().getCurrentUsername());
                        vsc.setContent(((TextView)(rootView.findViewById(R.id.commentInput))).getText().toString().trim());
                        ((MainContainer)getActivity()).getMapper().save(vsc);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                PostPage postPage = ((MainContainer)getActivity()).getPostPage();
                                PostPageAdapter m_adapter = postPage.getPPAdapter();
                                m_adapter.clearList();
                                //m_adapter.notifyDataSetChanged(); probably unnecessary since we'll be making new adapter in post page in setContent
                                postPage.setContent(post, false);

                                //TODO: refresh comments list (but not the post info part) of the PostPage when we return to it here
                                ((MainContainer)getActivity()).getViewPager().setCurrentItem(3);    //3 -> PostPage

                            }
                        });
                    }
                };
                Thread mythread = new Thread(runnable);
                mythread.start();
                Log.d("VSCOMMENT", "VSComment submitted");
            }
        });


        disableChildViews();
        return rootView;
    }

    public void setContent(String question, String x, String y, Post post){
        questionTV.setText(question);
        vsX.setText(x);
        vsY.setText(y);
        this.post = post;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("VISIBLE", "SEARCH VISIBLE");
            if(rootView != null)
                enableChildViews();
        }
        else {
            Log.d("VISIBLE", "SEARCH POST GONE");
            if (rootView != null)
                disableChildViews();
        }
    }

    public void enableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);
            childViews.get(i).setLayoutParams(LPStore.get(i));

        }
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }
}


