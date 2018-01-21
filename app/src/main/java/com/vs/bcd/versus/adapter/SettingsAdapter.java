package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SettingObject;

import java.util.List;


public class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity activity;
    private List<SettingObject> settingObjects;

    public SettingsAdapter(List<SettingObject> settingObjects, Activity activity) {
        this.settingObjects = settingObjects;
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.setting_object, parent, false);
        return new SettingObjectViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final SettingObject settingObject = settingObjects.get(position);
        SettingObjectViewHolder settingObjectViewHolder = (SettingObjectViewHolder) holder;

        //settingObjectViewHolder.settingIcon.setImageResource(settingObject.getIconResID());
        settingObjectViewHolder.settingName.setText(settingObject.getSettingName());

        switch(settingObject.getSettingName()){
            case "Log Out":
                settingObjectViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((MainContainer)activity).sessionLogOut();
                    }
                });
                break;
            case "Add 10 Posts":
                settingObjectViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Runnable runnable = new Runnable() {
                            public void run() {
                                for(int i = 0; i<10; i++){
                                    Post post = new Post();
                                    post.setCategory(5);
                                    post.setAuthor("Deeks");
                                    post.setRedname("red");
                                    post.setBlackname("blue");
                                    post.setQuestion("question?");
                                    post.setRedimg("default");
                                    post.setBlackimg("default");
                                    ((MainContainer)activity).getMapper().save(post);
                                }
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();

                    }
                });
                break;
        }

    }

    @Override
    public int getItemCount() {
        return settingObjects == null ? 0 : settingObjects.size();
    }

    private class SettingObjectViewHolder extends RecyclerView.ViewHolder {

        //public ImageView settingIcon;
        public TextView settingName;


        public SettingObjectViewHolder(View view) {
            super(view);
            //settingIcon = (ImageView) view.findViewById(R.id.setting_icon);
            settingName = (TextView) view.findViewById(R.id.setting_name);
        }
    }

    public void updateList(List<SettingObject> list){
        settingObjects = list;
        notifyDataSetChanged();
    }
}