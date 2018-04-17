package com.vs.bcd.versus.model;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;

import java.net.URL;

/**
 * Created by dlee on 2/18/18.
 */

public class GlideUrlCustom extends GlideUrl{

    public GlideUrlCustom(URL url){
        super(url);
    }

    @Override
    public String getCacheKey() {
        String url = toStringUrl();
        if (url.contains("?")) {
            return url.substring(0, url.lastIndexOf("?"));
        } else {
            return url;
        }
    }
}


