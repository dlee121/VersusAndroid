package com.vs.bcd.versus.model;

import java.util.ArrayList;
import java.util.HashMap;

import static android.R.attr.name;

/**
 * Created by dlee on 10/13/17.
 */

public class MedalUpdateRequest {

    private int p; //points increment
    private String id; //postID

    public MedalUpdateRequest() {
    }

    public MedalUpdateRequest(int p, String id) {
        this.p = p;
        this.id = id;
    }


    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
