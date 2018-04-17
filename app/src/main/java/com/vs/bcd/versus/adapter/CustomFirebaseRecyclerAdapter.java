package com.vs.bcd.versus.adapter;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.fragment.MessengerFragment;
import com.vs.bcd.versus.model.GlideApp;
import com.vs.bcd.versus.model.GlideUrlCustom;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.RNumAndUList;
import com.vs.bcd.versus.model.RoomObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by dlee on 4/12/18.
 */

public abstract class CustomFirebaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends FirebaseRecyclerAdapter<T, VH> implements ListPreloader.PreloadModelProvider<RoomObject>{

    public CustomFirebaseRecyclerAdapter(@NonNull FirebaseRecyclerOptions<T> options) {
        super(options);
    }

}
