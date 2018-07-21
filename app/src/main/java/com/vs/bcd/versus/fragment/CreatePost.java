package com.vs.bcd.versus.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.media.ExifInterface;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.system.ErrnoException;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.vs.bcd.api.model.PostEditModel;
import com.vs.bcd.api.model.PostEditModelDoc;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.GlideUrlCustom;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SquareImageView;

/**
 * Created by dlee on 5/19/17.
 */

public class CreatePost extends Fragment {

    private EditText rednameET, blacknameET, questionET;
    private TextView rednameTV, blacknameTV, questionTV, optionalLeft, optionalRight;
    //private CropImageView cropper1;
    //private CropImageView cropper2;
    private SquareImageView ivLeft, ivRight;
    private String redStr;
    private String blackStr;
    private String questiongStr;
    private Button categorySelectionButton;
    private int catInt;
    private String catStr;
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private SessionManager sessionManager;
    private Uri mCropImageUri;
    private AmazonS3 s3;
    private int cropperNumber = 1;
    private int redimgSet = 0;
    private int blackimgSet = 0;
    private MainContainer activity;
    private int currentCategorySelection = -1;
    private RequestOptions requestOptions;
    private ImageButton leftClearButton, rightClearButton;

    private final int HOME = 0;
    private final int TRENDING = 1;
    private final int CATEGORY = 2;
    private final int POSTPAGE = 3; //for edit post
    private int originFragNum = HOME;

    private int DEFAULT = 0;
    private int S3 = 1;

    private Toast mToast;

    private boolean leftImgEdited = false;
    private boolean leftImgDeleted = false;
    private boolean rightImgEdited = false;
    private boolean rightImgDeleted = false;
    private Post postToEdit;

    private boolean permissionFlag = false;

    private int imagesAdded = 0; //0 = none, 1 = left, 2 = right, 3 = both.

    private KeyListener qListener, rListener, bListener;

    private RelativeLayout.LayoutParams leftClearButtonLP, rightClearButtonLP, qETLP, qTVLP, rETLP, rTVLP, bETLP, bTVLP;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.create_post, container, false);

        s3 = ((MainContainer) getActivity()).getS3Client();
        optionalLeft = rootView.findViewById(R.id.optional_left);
        optionalRight = rootView.findViewById(R.id.optional_right);

        ivLeft = rootView.findViewById(R.id.leftImage);
        ivLeft.setDrawingCacheEnabled(true);
        leftClearButton = rootView.findViewById(R.id.left_image_clear);
        leftClearButtonLP = (RelativeLayout.LayoutParams) leftClearButton.getLayoutParams();
        hideLeftClearButton();

        ivRight = rootView.findViewById(R.id.rightImage);
        ivRight.setDrawingCacheEnabled(true);
        rightClearButton = rootView.findViewById(R.id.right_image_clear);
        rightClearButtonLP = (RelativeLayout.LayoutParams) rightClearButton.getLayoutParams();
        hideRightClearButton();

        rednameET = rootView.findViewById(R.id.redname_in);
        blacknameET = rootView.findViewById(R.id.blackname_in);
        questionET = rootView.findViewById(R.id.question_in);

        rednameTV = rootView.findViewById(R.id.redname_tv);
        blacknameTV = rootView.findViewById(R.id.blackname_tv);
        questionTV = rootView.findViewById(R.id.question_tv);

        qETLP = (RelativeLayout.LayoutParams) questionET.getLayoutParams();
        qTVLP = (RelativeLayout.LayoutParams) questionTV.getLayoutParams();
        rETLP = (RelativeLayout.LayoutParams) rednameET.getLayoutParams();
        rTVLP = (RelativeLayout.LayoutParams) rednameTV.getLayoutParams();
        bETLP = (RelativeLayout.LayoutParams) blacknameET.getLayoutParams();
        bTVLP = (RelativeLayout.LayoutParams) blacknameTV.getLayoutParams();

        qListener = questionET.getKeyListener();
        rListener = rednameET.getKeyListener();
        bListener = blacknameET.getKeyListener();

        categorySelectionButton = (Button) rootView.findViewById(R.id.go_to_catselect);
        sessionManager = new SessionManager(getActivity());
        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i < ((ViewGroup) rootView).getChildCount(); i++) {
            childViews.add(((ViewGroup) rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        requestOptions = new RequestOptions();
        requestOptions.skipMemoryCache(true);
        requestOptions.diskCacheStrategy(DiskCacheStrategy.NONE);


        ivLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.closeSoftKeyboard();
                cropperNumber = 1;      //TODO: will there be any race condition for this variable or any other bugs?
                onLoadImageClick();
            }
        });

        leftClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearLeftImage();
            }
        });

        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.closeSoftKeyboard();
                cropperNumber = 2;
                onLoadImageClick();
            }
        });

        rightClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearRightImage();
            }
        });

        categorySelectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.closeSoftKeyboard();
                activity.getViewPager().setCurrentItem(5);
            }
        });

        disableChildViews();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer) context;
    }

    public void setUpEditPage(Post post){
        setOriginFragNum(3);
        postToEdit = post;

        questionET.setText(".");
        questionET.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        questionET.setFocusable(false);
        rednameET.setText(".");
        rednameET.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        rednameET.setFocusable(false);
        blacknameET.setText(".");
        blacknameET.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        blacknameET.setFocusable(false);

        questionTV.setLayoutParams(qTVLP);
        rednameTV.setLayoutParams(rTVLP);
        blacknameTV.setLayoutParams(bTVLP);

        questionTV.setText(post.getQuestion());
        rednameTV.setText(post.getRedname());
        blacknameTV.setText(post.getBlackname());

        categorySelectionButton.setText(post.getCategoryString());
        currentCategorySelection = post.getCategory();
        if(post.getRedimg()%10 == S3){
            showLeftClearButton();
            try{
                GlideUrlCustom gurlLeft = new GlideUrlCustom(activity.getImgURI(post, 0));
                setScaleTypeForImageLoadLeft();
                Glide.with(this).load(gurlLeft).into(ivLeft);
            } catch (Exception e) {
                ivLeft.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_background));
            }
        }
        if(post.getBlackimg()%10 == S3){
            showRightClearButton();
            try{
                GlideUrlCustom gurlRight = new GlideUrlCustom(activity.getImgURI(post, 1));
                setScaleTypeForImageLoadRight();
                Glide.with(this).load(gurlRight).into(ivRight);
            } catch (Exception e) {
                ivRight.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_background));
            }
        }
    }

    public boolean createButtonPressed(){
        if(currentCategorySelection == -1){
            if(mToast != null){
                mToast.cancel();
            }
            mToast = Toast.makeText(activity, "Please select a category", Toast.LENGTH_SHORT);
            mToast.show();

            return false;
        }
        //this is where you validate data and, if valid, write to database
        //TODO: validate submission here
        redStr = rednameET.getText().toString();
        blackStr = blacknameET.getText().toString();
        questiongStr = questionET.getText().toString();

        if(!(questiongStr!=null&&questiongStr.length()>0)){
            if(mToast != null){
                mToast.cancel();
            }
            mToast = Toast.makeText(activity, "Please enter a question or topic for this post", Toast.LENGTH_SHORT);
            mToast.show();

            return false;
        }

        if(!(redStr!=null&&redStr.length()>0&&blackStr!=null&&blackStr.length()>0)){
            if(mToast != null){
                mToast.cancel();
            }
            mToast = Toast.makeText(activity, "Please enter what you'd like to compare (pictures optional)", Toast.LENGTH_SHORT);
            mToast.show();

            return false;
        }

        activity.showToolbarProgressbar();

        if(originFragNum == POSTPAGE && postToEdit != null){

            Runnable runnable = new Runnable() {
                public void run() {
                    boolean postEdited = false;
                    boolean waitForImageUpload = false;

                    if(postToEdit.getCategory() != (currentCategorySelection)){
                        postEdited = true;
                        postToEdit.setCategory(currentCategorySelection);
                    }
                    if(leftImgDeleted){
                        int editVersion = postToEdit.getRedimg()/10;
                        int newImg = editVersion * 10 + DEFAULT;
                        postEdited = true;
                        postToEdit.setRedimg(newImg);

                        final String objectKey;
                        if(editVersion > 0){//this image has edit version number
                            objectKey = postToEdit.getPost_id() + "-left" + Integer.toString(editVersion) + ".jpeg";
                        }
                        else{
                            objectKey = postToEdit.getPost_id() + "-left.jpeg";
                        }

                        Runnable runnable = new Runnable() {
                            public void run() {
                                activity.getS3Client().deleteObject("versus.pictures", objectKey);
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();
                    }
                    if(leftImgEdited){
                        int oldEditVersion = postToEdit.getRedimg()/10;

                        waitForImageUpload = true;
                        postEdited = true;
                        int newRedimg = (postToEdit.getRedimg()/10 + 1) * 10 + S3;
                        postToEdit.setRedimg(newRedimg);

                        final String objectKey;
                        if(oldEditVersion > 0){//this image has edit version number
                            objectKey = postToEdit.getPost_id() + "-left" + Integer.toString(oldEditVersion) + ".jpeg";
                        }
                        else{
                            objectKey = postToEdit.getPost_id() + "-left.jpeg";
                        }

                        Runnable runnable = new Runnable() {
                            public void run() {
                                activity.getS3Client().deleteObject("versus.pictures", objectKey);
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();


                    }
                    if(rightImgDeleted){
                        int editVersion = postToEdit.getBlackimg()/10;
                        int newImg = editVersion * 10 + DEFAULT;
                        postEdited = true;
                        postToEdit.setBlackimg(newImg);

                        final String objectKey;
                        if(editVersion > 0){//this image has edit version number
                            objectKey = postToEdit.getPost_id() + "-right" + Integer.toString(editVersion) + ".jpeg";
                        }
                        else{
                            objectKey = postToEdit.getPost_id() + "-right.jpeg";
                        }

                        Runnable runnable = new Runnable() {
                            public void run() {
                                activity.getS3Client().deleteObject("versus.pictures", objectKey);
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();
                    }
                    if(rightImgEdited){
                        int oldEditVersion = postToEdit.getBlackimg()/10;

                        waitForImageUpload = true;
                        postEdited = true;
                        int newBlackimg = (postToEdit.getBlackimg()/10 + 1) * 10 + S3;
                        postToEdit.setBlackimg(newBlackimg);

                        final String objectKey;
                        if(oldEditVersion > 0){//this image has edit version number
                            objectKey = postToEdit.getPost_id() + "-right" + Integer.toString(oldEditVersion) + ".jpeg";
                        }
                        else{
                            objectKey = postToEdit.getPost_id() + "-right.jpeg";
                        }

                        Runnable runnable = new Runnable() {
                            public void run() {
                                activity.getS3Client().deleteObject("versus.pictures", objectKey);
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();
                    }

                    //ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        if(leftImgEdited){
                            uploadImageToAWS(ivLeft.getDrawingCache(), postToEdit.getPost_id(), "left" + Integer.toString(postToEdit.getRedimg()/10));
                        }
                        if(rightImgEdited){
                            uploadImageToAWS(ivRight.getDrawingCache(), postToEdit.getPost_id(), "right" + Integer.toString(postToEdit.getBlackimg()/10));
                        }

                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
                    }

                    if(postEdited){
                        PostEditModel postEditModel = new PostEditModel();
                        PostEditModelDoc postEditModelDoc = new PostEditModelDoc();
                        postEditModelDoc.setBi(BigDecimal.valueOf(postToEdit.getBlackimg()));
                        postEditModelDoc.setC(BigDecimal.valueOf(postToEdit.getCategory()));
                        postEditModelDoc.setRi(BigDecimal.valueOf(postToEdit.getRedimg()));
                        postEditModel.setDoc(postEditModelDoc);

                        activity.getClient().posteditPost(postEditModel, "editp", postToEdit.getPost_id());

                        if(!waitForImageUpload){
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.getPostPage().setContent(postToEdit);
                                    activity.hideToolbarProgressbar();
                                    activity.getViewPager().setCurrentItem(3);
                                    //activity.setToolbarTitleTextForCP();
                                }
                            });
                        }
                    }
                    else{
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("postEdit", "nothing edited");
                                activity.hideToolbarProgressbar();
                                activity.getViewPager().setCurrentItem(3);
                                //activity.setToolbarTitleTextForCP();
                            }
                        });

                    }



                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();

            return true;
        }

        else{
            Runnable runnable = new Runnable() {
                public void run() {
                    final Post post = new Post();
                    boolean waitForImageUpload = false;
                    postToEdit = post;

                    //ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        if(redimgSet == S3){
                            waitForImageUpload = true;
                            uploadImageToAWS(ivLeft.getDrawingCache(), post.getPost_id(), "left");
                        }
                        if(blackimgSet == S3){
                            waitForImageUpload = true;
                            uploadImageToAWS(ivRight.getDrawingCache(), post.getPost_id(), "right");
                        }

                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
                    }

                    post.setCategory(currentCategorySelection);
                    post.setAuthor(sessionManager.getCurrentUsername());
                    post.setRedname(redStr);
                    post.setBlackname(blackStr);
                    post.setQuestion(questiongStr);
                    post.setRedimg(redimgSet);
                    post.setBlackimg(blackimgSet);
                    //activity.getMapper().save(post);
                    activity.getClient().postputPost(post.getPutModel(), post.getPost_id(), "put", "post");

                    if(!waitForImageUpload){
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(originFragNum == HOME || originFragNum == CATEGORY){
                                    activity.addPostToTop(post, originFragNum);
                                }
                                activity.setMyAdapterFragInt(0);
                                activity.getPostPage().setContent(post);
                                activity.hideToolbarProgressbar();
                                activity.getViewPager().setCurrentItem(3);
                                //activity.setToolbarTitleTextForCP();
                            }
                        });
                    }
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();

            return true;
        }
    }


    //TODO: fix below enabler/disabler to reflect new changed layout
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("VISIBLE", "CREATE POST VISIBLE");
            if(rootView != null){
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                enableChildViews();
                /*
                if(currentCategorySelection < 0){
                    InputMethodManager imgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    questionET.requestFocus();
                }
                */
            }
        }
        else {
            Log.d("VISIBLE", "CREATE POST GONE");
            if (rootView != null) {
                disableChildViews();
            }

        }
    }

    public void enableChildViews(){
        /* commented these out since resetCatSelection handles these operations now
        redimgSet = "default";
        blackimgSet = "default";
        */
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);
            childViews.get(i).setLayoutParams(LPStore.get(i));
        }
    }

    public void disableChildViews(){
        Log.d("disabling", "This many: " + Integer.toString(childViews.size()));
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new LinearLayout.LayoutParams(0,0));
        }
    }

    public void setOriginFragNum(int originFragNum){
        if(originFragNum != POSTPAGE){
            if(questionET.getKeyListener() == null){
                questionET.setKeyListener(qListener);
                rednameET.setKeyListener(rListener);
                blacknameET.setKeyListener(bListener);
            }
        }
        else{
            questionET.setKeyListener(null);
            rednameET.setKeyListener(null);
            blacknameET.setKeyListener(null);
        }

        resetCatSelection();
        this.originFragNum = originFragNum;
    }

    /**
     * On load image button click, start pick image chooser activity.
     */
    public void onLoadImageClick() {
        startActivityForResult(getPickImageChooserIntent(), 200);
    }


    private void uploadImageToAWS(final Bitmap bmpIn, final String postIDin, final String side) {

        //Log.d("IMGUPLOAD", postIDin + "-" + side + ".jpeg");

        AsyncTask<String, String, String> _Task = new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {

            }

            @Override
            protected String doInBackground(String... arg0)
            {
                //if (NetworkAvailablity.checkNetworkStatus(MyActivity.this))
                //{
                try {
                    java.util.Date expiration = new java.util.Date();
                    long msec = expiration.getTime();
                    msec += 1000 * 60 * 60; // 1 hour.
                    expiration.setTime(msec);
                    publishProgress(arg0);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bmpIn.compress(Bitmap.CompressFormat.JPEG, 65, bos);
                    byte[] bitmapdata = bos.toByteArray();
                    ByteArrayInputStream bis = new ByteArrayInputStream(bitmapdata);

                    String keyName = postIDin + "-" + side + ".jpeg";

                    ObjectMetadata meta = new ObjectMetadata();
                    meta.setContentLength(bitmapdata.length);

                    PutObjectRequest por = new PutObjectRequest("versus.pictures",
                            keyName,
                            bis,
                            meta);

                    //making the object Public
                    //por.setCannedAcl(CannedAccessControlList.PublicRead);

                    s3.putObject(por);


                    //run UI updates on UI Thread
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(side.substring(0,4).equals("left")){
                                ivLeft.setDrawingCacheEnabled(false);
                                ivLeft.setDrawingCacheEnabled(true);
                            }
                            else{
                                ivRight.setDrawingCacheEnabled(false);
                                ivRight.setDrawingCacheEnabled(true);
                            }
                        }
                    });

                    //String _finalUrl = "https://"+existingBucketName+".s3.amazonaws.com/" + keyName + ".jpeg";

                } catch (Exception e) {
                    // writing error to Log
                    e.printStackTrace();
                }
            /*
                }
                else
                {

                }
            */
                return null;
            }
            @Override
            protected void onProgressUpdate(String... values) {
                // TODO Auto-generated method stub
                super.onProgressUpdate(values);
                System.out.println("Progress : "  + values);
            }
            @Override
            protected void onPostExecute(String result)
            {
                switch (imagesAdded){

                    case 3: //both images are present
                        if(!side.substring(0,4).equals("left")){
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(originFragNum == HOME || originFragNum == CATEGORY){
                                        activity.addPostToTop(postToEdit, originFragNum);
                                    }
                                    activity.setMyAdapterFragInt(0);
                                    activity.getPostPage().setContent(postToEdit);
                                    activity.hideToolbarProgressbar();
                                    activity.getViewPager().setCurrentItem(3);
                                    //activity.setToolbarTitleTextForCP();
                                }
                            });
                        }
                        break;

                    default:
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(originFragNum == HOME || originFragNum == CATEGORY){
                                    activity.addPostToTop(postToEdit, originFragNum);
                                }
                                else if(originFragNum == POSTPAGE){
                                    activity.updateEditedPost(postToEdit);
                                }
                                activity.setMyAdapterFragInt(0);
                                activity.getPostPage().setContent(postToEdit);
                                activity.hideToolbarProgressbar();
                                activity.getViewPager().setCurrentItem(3);
                                //activity.setToolbarTitleTextForCP();
                            }
                        });
                        break;
                }
            }
        };
        _Task.execute((String[]) null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        permissionFlag = false;
        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = getPickImageResultUri(data);

            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            boolean requirePermissions = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {


                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true;
                mCropImageUri = imageUri;

                if(!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                    permissionFlag = true;
                }

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }

            if (!requirePermissions) {

                float rotate = 0;
                InputStream in = null;
                try {

                    ContentResolver resolver = getContext().getContentResolver();
                    in = resolver.openInputStream(imageUri);
                    ExifInterface exif = new ExifInterface(in);
                    // Now you can extract any Exif tag you want
                    // Assuming the image is a JPEG or supported raw format
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotate = 270;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotate = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotate = 90;
                            break;
                    }
                } catch (IOException e) {
                    // Handle any errors
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ignored) {}
                    }
                }

                if(cropperNumber == 1) {
                    //cropper1.setImageUriAsync(imageUri);
                    setScaleTypeForImageLoadLeft();
                    Glide.with(this).load(imageUri).apply(requestOptions).into(ivLeft);
                    redimgSet = S3;
                    leftImgEdited = true;
                    leftImgDeleted = false;
                    switch (imagesAdded){
                        case 0: //none
                            imagesAdded = 1;
                            break;
                        case 2: //right image is present
                            imagesAdded = 3;
                            break;
                    }
                    showLeftClearButton();
                }

                else {
                    //cropper2.setImageUriAsync(imageUri);
                    setScaleTypeForImageLoadRight();
                    Glide.with(this).load(imageUri).apply(requestOptions).into(ivRight);
                    blackimgSet = S3;
                    rightImgEdited = true;
                    rightImgDeleted = false;
                    switch (imagesAdded){
                        case 0: //none
                            imagesAdded = 2;
                            break;
                        case 1: //left image is present
                            imagesAdded = 3;
                            break;
                    }
                    showRightClearButton();
                }

                Log.d("cropper", "Cropper Number: " + Integer.toString(cropperNumber) + ", URI: " + imageUri.toString());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if(cropperNumber == 1){
                //cropper1.setImageUriAsync(mCropImageUri);
                setScaleTypeForImageLoadLeft();
                Glide.with(this).load(mCropImageUri).apply(requestOptions).into(ivLeft);
                redimgSet = S3;
                leftImgEdited = true;
                leftImgDeleted = false;
                switch (imagesAdded){
                    case 0: //none
                        imagesAdded = 1;
                        break;
                    case 2: //right image is present
                        imagesAdded = 3;
                        break;
                }
                showLeftClearButton();

            }
            else{
                setScaleTypeForImageLoadRight();
                Glide.with(this).load(mCropImageUri).apply(requestOptions).into(ivRight);
                blackimgSet = S3;
                rightImgEdited = true;
                rightImgDeleted = false;
                switch (imagesAdded) {
                    case 0: //none
                        imagesAdded = 2;
                        break;
                    case 1: //left image is present
                        imagesAdded = 3;
                        break;
                }
                showRightClearButton();
            }


        } else if(permissionFlag && Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(permissions[0])){
            Toast.makeText(getActivity(), "Permission to read external storage was denied. Please enable it in the settings.", Toast.LENGTH_LONG).show();
            Intent i = new Intent();
            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setData(Uri.parse("package:" + activity.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            activity.startActivity(i);
        } else{
            Toast.makeText(getActivity(), "Required permission was not granted.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Create a chooser intent to select the source to get image from.<br/>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br/>
     * All possible sources are added to the intent chooser.
     */
    public Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getContext().getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    /**
     * Get URI to image received from capture by camera.
     */
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getContext().getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }

    /**
     * Get the URI of the selected image from {@link #getPickImageChooserIntent()}.<br/>
     * Will return the correct URI for camera and gallery image.
     *
     * @param data the returned data of the activity result
     */
    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null && data.getData() != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    /**
     * Test if we can open the given Android URI to test if permission required error is thrown.<br>
     */
    public boolean isUriRequiresPermissions(Uri uri) {
        try {
            ContentResolver resolver = getContext().getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            stream.close();
            return false;
        } catch (FileNotFoundException e) {
            if (e.getCause() instanceof ErrnoException) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void resetCatSelection(){
        currentCategorySelection = -1;
        cropperNumber = 1;
        redimgSet = DEFAULT;
        blackimgSet = DEFAULT;
        setScaleTypeForEmptyViewLeft();
        ivLeft.setImageResource(R.drawable.plus_blue);
        setScaleTypeForEmptyViewRight();
        ivRight.setImageResource(R.drawable.plus_blue);

        questionET.setLayoutParams(qETLP);
        questionET.setFocusableInTouchMode(true);
        questionET.setFocusable(true);
        rednameET.setLayoutParams(rETLP);
        rednameET.setFocusableInTouchMode(true);
        rednameET.setFocusable(true);
        blacknameET.setLayoutParams(bETLP);
        blacknameET.setFocusableInTouchMode(true);
        blacknameET.setFocusable(true);

        questionTV.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        questionTV.setText("");
        rednameTV.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        rednameTV.setText("");
        blacknameTV.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        blacknameTV.setText("");

        rednameET.setText("");
        blacknameET.setText("");
        questionET.setText("");

        categorySelectionButton.setText("Select Category");
        leftImgEdited = false;
        leftImgDeleted = false;
        rightImgEdited = false;
        rightImgDeleted = false;
        postToEdit = null;
        imagesAdded = 0;
        hideLeftClearButton();
        hideRightClearButton();
        activity.getPostPage().clearList();

        permissionFlag = false;
    }

    public void setCatSelection(String catName, int catSelection){
        categorySelectionButton.setText(catName);
        currentCategorySelection = catSelection;
    }

    private void hideLeftClearButton(){
        leftClearButton.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        leftClearButton.setEnabled(false);
        optionalLeft.setVisibility(View.VISIBLE);
    }

    private void hideRightClearButton(){
        rightClearButton.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        rightClearButton.setEnabled(false);
        optionalRight.setVisibility(View.VISIBLE);
    }

    private void showLeftClearButton(){
        leftClearButton.setEnabled(true);
        leftClearButton.setLayoutParams(leftClearButtonLP);
        optionalLeft.setVisibility(View.GONE);
    }

    private void showRightClearButton(){
        rightClearButton.setEnabled(true);
        rightClearButton.setLayoutParams(rightClearButtonLP);
        optionalRight.setVisibility(View.GONE);
    }

    private void clearLeftImage(){
        hideLeftClearButton();
        switch (imagesAdded) {
            case 1: //left image is present
                imagesAdded = 0;
                break;
            case 3: //both images are present
                imagesAdded = 2;
                break;
        }

        redimgSet = DEFAULT;
        leftImgEdited = false;
        leftImgDeleted = true;

        ivLeft.setDrawingCacheEnabled(false);
        ivLeft.setDrawingCacheEnabled(true);
        setScaleTypeForEmptyViewLeft();
        ivLeft.setImageResource(R.drawable.plus_blue);
    }

    private void clearRightImage() {
        hideRightClearButton();
        switch (imagesAdded) {
            case 2: //right image is present
                imagesAdded = 0;
                break;
            case 3: //both images are present
                imagesAdded = 1;
                break;
        }

        blackimgSet = DEFAULT;
        rightImgEdited = false;
        rightImgDeleted = true;

        ivRight.setDrawingCacheEnabled(false);
        ivRight.setDrawingCacheEnabled(true);
        setScaleTypeForEmptyViewRight();
        ivRight.setImageResource(R.drawable.plus_blue);
    }

    private void setScaleTypeForImageLoadLeft(){
        ivLeft.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }
    private void setScaleTypeForImageLoadRight(){
        ivRight.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }
    private void setScaleTypeForEmptyViewLeft(){
        ivLeft.setScaleType(ImageView.ScaleType.CENTER);
    }
    private void setScaleTypeForEmptyViewRight(){
        ivRight.setScaleType(ImageView.ScaleType.CENTER);
    }

}