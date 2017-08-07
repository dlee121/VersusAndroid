package com.vs.bcd.versus.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
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
import android.support.v4.app.Fragment;
import android.system.ErrnoException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.adapter.CategoriesAdapter;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.Post;

/**
 * Created by dlee on 5/19/17.
 */

public class CreatePost extends Fragment {

    private EditText rednameET;
    private EditText blacknameET;
    private EditText questionET;
    //private CropImageView cropper1;
    //private CropImageView cropper2;
    private ImageView ivLeft, ivRight;
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
    private String redimgSet = "default";
    private String blackimgSet = "default";
    private MainContainer activity;
    private int currentCategorySelection = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.create_post, container, false);

        activity = (MainContainer)getActivity();
        s3 = ((MainContainer)getActivity()).getS3Client();

        //cropper1 = (CropImageView)rootView.findViewById(R.id.CropImageView1);
        //cropper2 = (CropImageView)rootView.findViewById(R.id.CropImageView2);
        ivLeft = (ImageView)rootView.findViewById(R.id.leftImage);
        ivLeft.setDrawingCacheEnabled(true);
        ivRight = (ImageView)rootView.findViewById(R.id.rightImage);
        ivRight.setDrawingCacheEnabled(true);
        rednameET = (EditText)rootView.findViewById(R.id.redname_in);
        blacknameET = (EditText)rootView.findViewById(R.id.blackname_in);
        questionET = (EditText)rootView.findViewById(R.id.question_in);
        categorySelectionButton = (Button)rootView.findViewById(R.id.go_to_catselect);
        sessionManager = new SessionManager(getActivity());
        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }
        childViews.add(ivLeft);
        LPStore.add(ivLeft.getLayoutParams());
        childViews.add(ivRight);
        LPStore.add(ivRight.getLayoutParams());


        ivLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropperNumber = 1;      //TODO: will there be any race condition for this variable or any other bugs?
                onLoadImageClick();
            }
        });

        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropperNumber = 2;
                onLoadImageClick();
            }
        });

        categorySelectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getViewPager().setCurrentItem(5);
            }
        });



        disableChildViews();

        return rootView;
    }

    public void createButtonPressed(View view){
        if(currentCategorySelection == -1){
            //TODO: warning message telling user to pick a category.
            return;
        }
        //this is where you validate data and, if valid, write to database
        //TODO: validate submission here
        redStr = rednameET.getText().toString();
        blackStr = blacknameET.getText().toString();
        questiongStr = questionET.getText().toString();

        Runnable runnable = new Runnable() {
            public void run() {
                final Post post = new Post();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    uploadImageToAWS(ivLeft.getDrawingCache(), post.getPost_id(), "left");
                    uploadImageToAWS(ivRight.getDrawingCache(), post.getPost_id(), "right");

                    //clear out drawing cache so that we can use it again with different images for another upload
                    ivLeft.setDrawingCacheEnabled(false);
                    ivRight.setDrawingCacheEnabled(false);
                    ivLeft.setDrawingCacheEnabled(true);
                    ivRight.setDrawingCacheEnabled(true);

                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
                }

                post.setCategory(currentCategorySelection);
                /*
                //time is now set in the constructor. refer to Post.java

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                post.setTime(df.format(new Date()));
                */
                post.setAuthor(sessionManager.getCurrentUsername());
                post.setRedname(redStr);
                post.setBlackname(blackStr);
                post.setQuestion(questiongStr);
                post.setRedimg(redimgSet);
                post.setBlackimg(blackimgSet);
                ((MainContainer)getActivity()).getMapper().save(post);



                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.getPostPage().setContent(post, true);
                        activity.getViewPager().setCurrentItem(3);
                        //getActivity().overridePendingTransition(0, 0);
                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }


    //TODO: fix below enabler/disabler to reflect new changed layout
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("VISIBLE", "CREATE POST VISIBLE");
            if(rootView != null)
                enableChildViews();
        }
        else {
            Log.d("VISIBLE", "CREATE POST GONE");
            if (rootView != null)
                disableChildViews();
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

            }
        };
        _Task.execute((String[]) null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = getPickImageResultUri(data);

            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            boolean requirePermissions = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    isUriRequiresPermissions(imageUri)) {

                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true;
                mCropImageUri = imageUri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }

            if (!requirePermissions) {
                if(cropperNumber == 1) {
                    //cropper1.setImageUriAsync(imageUri);
                    ivLeft.setImageURI(imageUri);
                    redimgSet = "s3";
                }

                else {
                    //cropper2.setImageUriAsync(imageUri);
                    ivRight.setImageURI(imageUri);
                    blackimgSet = "s3";
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
                ivLeft.setImageURI(mCropImageUri);
                redimgSet = "s3";
            }
            else{
                ///cropper2.setImageUriAsync(mCropImageUri);
                ivRight.setImageURI(mCropImageUri);
                blackimgSet = "s3";
            }
        } else {
            Toast.makeText(getActivity(), "Required permissions are not granted", Toast.LENGTH_LONG).show();
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
        redimgSet = "default";
        blackimgSet = "default";
        ivLeft.setImageResource(R.drawable.ic_add_24dp);
        ivRight.setImageResource(R.drawable.ic_add_24dp);
        rednameET.setText("");
        blacknameET.setText("");
        questionET.setText("");
        categorySelectionButton.setText("Select Category");
        activity.getPostPage().clearList();
    }

    public void setCatSelection(String catName, int catSelection){
        categorySelectionButton.setText(catName);
        currentCategorySelection = catSelection;
    }

}
