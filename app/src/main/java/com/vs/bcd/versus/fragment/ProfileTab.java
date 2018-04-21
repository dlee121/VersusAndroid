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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.media.ExifInterface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.system.ErrnoException;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.HttpGet;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.GlideUrlCustom;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by dlee on 4/29/17.
 */

public class ProfileTab extends Fragment {

    private MainContainer activity;
    private TextView usernameTV, goldTV, silverTV, bronzeTV, pointsTV, followerCountTV, followingCountTV;
    private Button followButton;
    private ImageButton messageButton;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private RelativeLayout.LayoutParams mainCaseLP, followCaseLP, medalCaseLP, progressbarLP, swipeLayoutLP, tabsLP, viewpagerLP;
    private LinearLayout.LayoutParams followbuttonLP, messageButtonLP;
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private int commentsORposts = 0;    //0 = comments, 1 = posts
    private final int COMMENTS = 0;
    private final int POSTS = 1;
    private String profileUsername = null; //username of the user for the profile page, not necessarily the current logged-in user
    private DatabaseReference mFirebaseDatabaseReference;
    private long followingCount = 0;
    private long followerCount = 0;

    private ProfileTab.SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private CommentsHistory commentsTab;
    private PostsHistory postsTab;

    private CircleImageView profileImageView;
    private Uri mImageUri;
    private RequestOptions requestOptions;
    private FloatingActionButton profileImgCancel, profileImgConfirm;
    private RelativeLayout.LayoutParams profileImgCancelLP, profileImgConfirmLP, uploadProgressBarLP;
    private ProgressBar uploadProgressBar;

    private String host, region;

    private int profileImgVersion = 0;
    private Drawable defaultProfileImage;

    private boolean followingThisUser = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.profile, container, false);

        host = activity.getESHost();
        region = activity.getESRegion();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new ProfileTab.SectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = rootView.findViewById(R.id.history_container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);

        tabLayout = rootView.findViewById(R.id.tabs_profile);
        //tabLayout.addTab(tabLayout.newTab().setText("COMMENTS"), true);
        //tabLayout.addTab(tabLayout.newTab().setText("POSTS"));
        tabsLP = (RelativeLayout.LayoutParams) tabLayout.getLayoutParams();
        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.getTabAt(0).setText("Comments");
        tabLayout.getTabAt(1).setText("Posts");
        //tabLayout.setBackgroundColor(getResources().getColor(R.color.vsBlue));
        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.vsRed));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: //comments history
                        commentsORposts = COMMENTS;
                        //mViewPager.setCurrentItem(0);
                        break;
                    case 1: //posts history
                        commentsORposts = POSTS;
                        //mViewPager.setCurrentItem(1);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        defaultProfileImage = ContextCompat.getDrawable(activity, R.drawable.default_profile);

        profileImageView = rootView.findViewById(R.id.profile_image_pt);
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(profileUsername != null && profileUsername.equals(activity.getUsername())){
                    uploadProfileImage();
                }
            }
        });

        profileImgCancel = rootView.findViewById(R.id.profile_img_cancel);
        profileImgCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideProfileImgButtons();
                loadProfileImage(profileUsername);
            }
        });
        profileImgCancelLP = (RelativeLayout.LayoutParams) profileImgCancel.getLayoutParams();

        profileImgConfirm = rootView.findViewById(R.id.profile_img_confirm);
        profileImgConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideProfileImgButtons();
                showUploadProgressBar();
                uploadImageToAWS(profileImageView.getDrawingCache());
            }
        });
        profileImgConfirmLP = (RelativeLayout.LayoutParams) profileImgConfirm.getLayoutParams();

        uploadProgressBar = rootView.findViewById(R.id.upload_progress_bar);
        uploadProgressBarLP = (RelativeLayout.LayoutParams) uploadProgressBar.getLayoutParams();

        usernameTV = rootView.findViewById(R.id.username_pt);
        goldTV = rootView.findViewById(R.id.pmc_goldmedal_count);
        silverTV = rootView.findViewById(R.id.pmc_silvermedal_count);
        bronzeTV = rootView.findViewById(R.id.pmc_bronzemedal_count);
        pointsTV = rootView.findViewById(R.id.points_pt);

        followerCountTV = rootView.findViewById(R.id.num_followers);
        followingCountTV = rootView.findViewById(R.id.num_following);

        messageButton = rootView.findViewById(R.id.profile_message_button);
        messageButtonLP = (LinearLayout.LayoutParams) messageButton.getLayoutParams();

        followButton = rootView.findViewById(R.id.followbutton);
        followbuttonLP = (LinearLayout.LayoutParams) followButton.getLayoutParams();
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(followingThisUser){
                    unfollowThisUser();
                }
                else{
                    followThisUser();
                }
            }
        });

        viewpagerLP = (RelativeLayout.LayoutParams) mViewPager.getLayoutParams();

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        disableChildViews();

        requestOptions = new RequestOptions();
        requestOptions.skipMemoryCache(true);
        requestOptions.diskCacheStrategy(DiskCacheStrategy.NONE);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        hideProfileImgButtons();
        hideUploadProgressBar();
        profileImageView.setDrawingCacheEnabled(false);
        profileImageView.setDrawingCacheEnabled(true);

        return rootView;
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            //Return current tabs
            switch (position) {
                case 0:
                    commentsTab = new CommentsHistory();
                    return commentsTab;
                case 1:
                    postsTab = new PostsHistory();
                    return postsTab;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer)context;
    }

    private void uploadProfileImage(){
        profileImageView.setDrawingCacheEnabled(false);
        profileImageView.setDrawingCacheEnabled(true);
        startActivityForResult(getPickImageChooserIntent(), 200);
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
                mImageUri = imageUri;
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

                Glide.with(this).load(imageUri).apply(requestOptions).into(profileImageView);
                showProfileImgButtons();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Glide.with(this).load(mImageUri).apply(requestOptions).into(profileImageView);
            showProfileImgButtons();

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

    private void uploadImageToAWS(final Bitmap bmpIn) {

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
                    String keyName = profileUsername + "-" + Integer.toString(profileImgVersion + 1) + ".jpeg";

                    ObjectMetadata meta = new ObjectMetadata();
                    meta.setContentLength(bitmapdata.length);

                    PutObjectRequest por = new PutObjectRequest("versus.profile-pictures",
                            keyName,
                            bis,
                            meta);

                    //making the object Public
                    //por.setCannedAcl(CannedAccessControlList.PublicRead);

                    activity.getS3Client().putObject(por);

                    //deletes previous profile image from S3, if any
                    if(profileImgVersion > 0){
                        String objectKey = profileUsername + "-" + Integer.toString(profileImgVersion) + ".jpeg";
                        activity.getS3Client().deleteObject("versus.profile-pictures", objectKey);
                    }

                    //update attribute "pi", which is profile image version
                    //update comment content through ddb update request
                    HashMap<String, AttributeValue> keyMap = new HashMap<>();
                    keyMap.put("i", new AttributeValue().withS(activity.getUsername()));

                    HashMap<String, AttributeValueUpdate> updates = new HashMap<>();

                    AttributeValueUpdate pi = new AttributeValueUpdate()
                            .withValue(new AttributeValue().withN("1"))
                            .withAction(AttributeAction.ADD);
                    updates.put("pi", pi);

                    UpdateItemRequest request = new UpdateItemRequest()
                            .withTableName("user")
                            .withKey(keyMap)
                            .withAttributeUpdates(updates);
                    activity.getDDBClient().updateItem(request);

                    /*
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
                    */

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
                //TODO: handle errors, and if picture was not uploaded then don't increment profileImgVersion
                profileImgVersion++;
                Toast.makeText(activity, "Profile image updated!", Toast.LENGTH_SHORT).show();
                hideUploadProgressBar();
                activity.getSessionManager().setProfileImage(profileImgVersion);
            }
        };
        _Task.execute((String[]) null);
    }

    private void showUploadProgressBar(){
        uploadProgressBar.setLayoutParams(uploadProgressBarLP);
    }

    private void hideUploadProgressBar(){
        uploadProgressBar.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
    }

    private void showProfileImgButtons(){
        Log.d("hohoy", "show them");
        profileImgCancel.setLayoutParams(profileImgCancelLP);
        profileImgConfirm.setLayoutParams(profileImgConfirmLP);
    }

    private void hideProfileImgButtons(){
        Log.d("hohoy", "hide them");
        profileImgCancel.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        profileImgConfirm.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
    }

    private void loadProfileImage(final String username) {

        //Log.d("IMGUPLOAD", postIDin + "-" + side + ".jpeg");

        AsyncTask<String, String, String> _Task = new AsyncTask<String, String, String>() {

            GlideUrlCustom imageURL;

            @Override
            protected void onPreExecute() {

            }

            @Override
            protected String doInBackground(String... arg0)
            {
                //if (NetworkAvailablity.checkNetworkStatus(MyActivity.this))
                //{
                try {

                    if(profileUsername.equals(activity.getUsername())){
                        profileImgVersion = activity.getUserProfileImageVersion();
                    }
                    else{
                        profileImgVersion = getProfileImgVersion(username);
                    }

                    activity.addToCentralProfileImgVersionMap(username, profileImgVersion);

                    if(profileImgVersion == 0){
                        imageURL = null;
                    }
                    else{
                        imageURL = activity.getProfileImgUrl(username, profileImgVersion);
                    }

                } catch (Exception e) {
                    // writing error to Log
                    e.printStackTrace();
                }
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
                if(imageURL != null){
                    Glide.with(activity.getProfileTab()).load(imageURL).into(profileImageView);
                }
                else{
                    Glide.with(activity.getProfileTab()).load(defaultProfileImage).into(profileImageView);
                }
            }
        };
        _Task.execute((String[]) null);
    }



    private int getProfileImgVersion(String username){

        String query = "/user/user_type/"+username;
        String url = "https://" + host + query;

        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", host);

        AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                .regionName(region)
                .serviceName("es") // es - elastic search. use your service name
                .httpMethodName("GET") //GET, PUT, POST, DELETE, etc...
                .canonicalURI(query) //end point
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .debug() // turn on the debug mode
                .build();

        HttpGet httpGet = new HttpGet(url);

		        /* Get header calculated for request */
        Map<String, String> header = aWSV4Auth.getHeaders();
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();

			    /* Attach header in your request */
			    /* Simple get request */

            httpGet.addHeader(key, value);
        }

        /* Create object of CloseableHttpClient */
        CloseableHttpClient httpClient = HttpClients.createDefault();

		/* Response handler for after request execution */
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
				/* Get status code */
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
					/* Convert response to String */
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        try {
			/* Execute URL and attach after execution response handler */

            String strResponse = httpClient.execute(httpGet, responseHandler);

            JSONObject obj = new JSONObject(strResponse);
            JSONObject item = obj.getJSONObject("_source");
            return item.getInt("pi");

            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //if the ES GET fails, then return old topCardContent
        return 0;
    }

    //for accessing another user's profile page
    public void setUpProfile(final String username, boolean myProfile){

        profileUsername = username;

        loadProfileImage(profileUsername);

        commentsTab.setProfileUsername(username);
        postsTab.setProfileUsername(username);

        if(myProfile){
            //this is setting up the profile page for the logged-in user, as in "Me" page
            //disable toolbarButtonLeft
            //use projection attribute to reduce network traffic; get posts list and comments list from SharedPref
            //so only grab: num_g, num_s, num_b, points

            Log.d("ptab", "setting up my profile");

            hideFollowUI();


            Runnable runnable = new Runnable() {
                public void run() {

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getGSBP(activity.getUserPath());
                            usernameTV.setText(username);
                            String followingText = Integer.toString(activity.getFollowingNum()) + "\nFollowing";
                            followingCountTV.setText(followingText);
                            String followersText = Integer.toString(activity.getFollowerNum()) + "\nFollowers";
                            followerCountTV.setText(followersText);
                        }
                    });
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();

        }
        else{
            hideProfileImgButtons();
            hideUploadProgressBar();
            //this is setting up the profile page for another user that the logged-in user clicked on
            //enable toolbarButtonLeft and set it to "x" or "<" and set it to go back to the page that user came from
            //use projection attribute to exclude private info.
            //so only grab: comments list, posts list, first name, last name, num_g, num_s, num_b, points
            Log.d("ptab", "setting up another user's profile");

            Log.d("ptab", "setting up my profile");

            if(activity.isFollowing(username)){
                showFollowedButton();
            }
            else{
                showFollowButton();
            }

            Runnable runnable = new Runnable() {
                public void run() {

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int usernameHash;
                            if(username.length() < 5){
                                usernameHash = username.hashCode();
                            }
                            else{
                                String hashIn = "" + username.charAt(0) + username.charAt(username.length() - 2) + username.charAt(1) + username.charAt(username.length() - 1);
                                usernameHash = hashIn.hashCode();
                            }
                            String userPath = Integer.toString(usernameHash) + "/" + username + "/";
                            getGSBP(userPath);
                            usernameTV.setText(username);
                            getFGHCounts();
                        }
                    });
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();
        }
    }

    public void openProfileWithCommentsTabSelected(){
        commentsORposts = COMMENTS;
        mViewPager.setCurrentItem(0);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                if(commentsORposts == POSTS){
                    mViewPager.setCurrentItem(1);
                }
                else{
                    commentsORposts = COMMENTS;
                    mViewPager.setCurrentItem(0);
                }
                enableChildViews();
            }
        }
        else{
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
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }

    //restore my profile
    public void restoreUI(){
        activity.meClickTrue();

        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(LPStore.get(i));
        }
    }

    private void showFollowedButton(){
        /*
        followingTextTV.setEnabled(true);
        followingTextTV.setVisibility(View.VISIBLE);
        followingTextTV.setLayoutParams(followingtextLP);

        checkmark.setEnabled(true);
        checkmark.setVisibility(View.VISIBLE);
        checkmark.setLayoutParams(checkmarkLP);

        messageButton.setEnabled(true);
        messageButton.setClickable(true);
        messageButton.setVisibility(View.VISIBLE);
        messageButton.setLayoutParams(messageButtonLP);

        followButton.setEnabled(false);
        followButton.setClickable(false);
        followButton.setVisibility(View.INVISIBLE);
        followButton.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        */

        followingThisUser = true;

        messageButton.setEnabled(true);
        messageButton.setClickable(true);
        messageButton.setVisibility(View.VISIBLE);
        messageButton.setLayoutParams(messageButtonLP);

        followButton.setEnabled(true);
        followButton.setClickable(true);
        followButton.setVisibility(View.VISIBLE);
        followButton.setLayoutParams(followbuttonLP);
        followButton.setText("Followed");
        followButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    }

    private void showFollowButton(){
        /*
        followingTextTV.setEnabled(false);
        followingTextTV.setVisibility(View.INVISIBLE);
        followingTextTV.setLayoutParams(new RelativeLayout.LayoutParams(0,0));

        checkmark.setEnabled(false);
        checkmark.setVisibility(View.INVISIBLE);
        checkmark.setLayoutParams(new RelativeLayout.LayoutParams(0,0));

        followButton.setEnabled(true);
        followButton.setClickable(true);
        followButton.setVisibility(View.VISIBLE);
        followButton.setLayoutParams(followbuttonLP);

        messageButton.setEnabled(true);
        messageButton.setClickable(true);
        messageButton.setVisibility(View.VISIBLE);
        messageButton.setLayoutParams(messageButtonLP);
        */

        followingThisUser = false;

        messageButton.setEnabled(true);
        messageButton.setClickable(true);
        messageButton.setVisibility(View.VISIBLE);
        messageButton.setLayoutParams(messageButtonLP);

        followButton.setEnabled(true);
        followButton.setClickable(true);
        followButton.setVisibility(View.VISIBLE);
        followButton.setLayoutParams(followbuttonLP);
        followButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        followButton.setText("Follow");
    }

    private void hideFollowUI(){

        followButton.setEnabled(false);
        followButton.setClickable(false);
        followButton.setVisibility(View.INVISIBLE);
        followButton.setLayoutParams(new LinearLayout.LayoutParams(0,0));

        messageButton.setEnabled(false);
        messageButton.setClickable(false);
        messageButton.setVisibility(View.INVISIBLE);
        messageButton.setLayoutParams(new LinearLayout.LayoutParams(0,0));
    }

    private int getUsernameHash(String username){
        int usernameHash;
        if(username.length() < 5){
            usernameHash = username.hashCode();
        }
        else {
            String hashIn = "" + username.charAt(0) + username.charAt(username.length() - 2) + username.charAt(1) + username.charAt(username.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        return usernameHash;
    }

    private void unfollowThisUser(){
        if(profileUsername != null){
            if(activity.getCreateMessageFragment().followingAndFollowedBy(profileUsername)) { //is in hPath

                //remove target from user's h list
                String userHPath = activity.getUserPath() + "h";
                mFirebaseDatabaseReference.child(userHPath).child(profileUsername).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //remove user from target's h list
                        String targetHPath = Integer.toString(getUsernameHash(profileUsername)) + "/" + profileUsername + "/h";
                        mFirebaseDatabaseReference.child(targetHPath).child(activity.getUsername()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //add target to user's f list
                                mFirebaseDatabaseReference.child(activity.getUserPath()+"f/"+profileUsername).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        showFollowButton();
                                        getFGHCounts();
                                    }
                                });

                                //add user to target's g list
                                mFirebaseDatabaseReference.child(Integer.toString(getUsernameHash(profileUsername)) + "/" + profileUsername + "/g/" + activity.getUsername()).setValue(true);
                            }
                        });
                    }
                });

            }
            else{ //is in gPath
                //remove target from user's gPath
                mFirebaseDatabaseReference.child(activity.getUserPath()+"g/"+profileUsername).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        showFollowButton();
                        getFGHCounts();
                    }
                });

                //remove user from target's fPath
                String targetFPath = Integer.toString(getUsernameHash(profileUsername))+"/"+profileUsername+"/f/"+activity.getUsername();
                mFirebaseDatabaseReference.child(targetFPath).removeValue();

                //remove contacts item for both users
                String targetContactsPath = Integer.toString(getUsernameHash(profileUsername))+"/"+profileUsername+"/contacts/"+activity.getUsername();
                mFirebaseDatabaseReference.child(targetContactsPath).removeValue();
                String userContactsPath = activity.getUserPath()+"contacts/"+profileUsername;
                mFirebaseDatabaseReference.child(userContactsPath).removeValue();
            }
        }


    }

    private void followThisUser(){
        if(profileUsername != null){
            if(activity.followedBy(profileUsername)) {   //add to h

                //add to current user's h list
                String userHPath = activity.getUserPath() + "h";
                mFirebaseDatabaseReference.child(userHPath).child(profileUsername)
                        .setValue(true, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                                   DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Log.w("MESSENGER", "Unable to update followings list in Firebase.");
                                }
                            }
                        });

                //remove old entry in f list now that we have it in h list
                String fPath = activity.getUserPath() + "f";
                mFirebaseDatabaseReference.child(fPath).child(profileUsername).removeValue();

                //update the followed user's h list
                int usernameHash;
                if(profileUsername.length() < 5){
                    usernameHash = profileUsername.hashCode();
                }
                else{
                    String hashIn = "" + profileUsername.charAt(0) + profileUsername.charAt(profileUsername.length() - 2) + profileUsername.charAt(1) + profileUsername.charAt(profileUsername.length() - 1);
                    usernameHash = hashIn.hashCode();
                }
                String hPath = Integer.toString(usernameHash) + "/" + profileUsername + "/h";
                mFirebaseDatabaseReference.child(hPath).child(activity.getUsername())
                    .setValue(true, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError,
                                               DatabaseReference databaseReference) {
                            if (databaseError == null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sendFollowNotification(profileUsername);
                                        showFollowedButton();
                                        getFGHCounts();
                                    }
                                });
                            } else {
                                Log.w("MESSENGER", "Unable to update followers list in Firebase.");
                            }
                        }
                    });

                //remove old entry in g list now that we have it in h list
                String gPath = Integer.toString(usernameHash) + "/" + profileUsername + "/g";
                mFirebaseDatabaseReference.child(gPath).child(activity.getUsername()).removeValue();

            }
            else{   //add to f and g

                //update the current user's following list in Firebase
                String followingsPath = activity.getUserPath() + "g";
                mFirebaseDatabaseReference.child(followingsPath).child(profileUsername)
                        .setValue(true, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                                   DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Log.w("MESSENGER", "Unable to update followings list in Firebase.");
                                }
                            }
                        });

                //update the followed user's follower list in Firebase
                int usernameHash;
                if(profileUsername.length() < 5){
                    usernameHash = profileUsername.hashCode();
                }
                else{
                    String hashIn = "" + profileUsername.charAt(0) + profileUsername.charAt(profileUsername.length() - 2) + profileUsername.charAt(1) + profileUsername.charAt(profileUsername.length() - 1);
                    usernameHash = hashIn.hashCode();
                }
                String followersPath = Integer.toString(usernameHash) + "/" + profileUsername + "/f";
                mFirebaseDatabaseReference.child(followersPath).child(activity.getUsername())
                    .setValue(true, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError,
                                               DatabaseReference databaseReference) {
                            if (databaseError == null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sendFollowNotification(profileUsername);
                                        showFollowedButton();
                                        getFGHCounts();
                                    }
                                });
                            } else {
                                Log.w("MESSENGER", "Unable to update followers list in Firebase.");
                            }
                        }
                    });

                //add contacts item for both users
                String targetContactsPath = Integer.toString(getUsernameHash(profileUsername))+"/"+profileUsername+"/contacts/"+activity.getUsername();
                mFirebaseDatabaseReference.child(targetContactsPath).setValue(true);
                String userContactsPath = activity.getUserPath()+"contacts/"+profileUsername;
                mFirebaseDatabaseReference.child(userContactsPath).setValue(true);

            }
        }
    }

    //get counts for following and follower
    private void getFGHCounts(){

        followingCount = 0;
        followerCount = 0;

        int usernameHash;
        if(profileUsername.length() < 5){
            usernameHash = profileUsername.hashCode();
        }
        else{
            String hashIn = "" + profileUsername.charAt(0) + profileUsername.charAt(profileUsername.length() - 2) + profileUsername.charAt(1) + profileUsername.charAt(profileUsername.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        final String fPath = usernameHash + "/" + profileUsername + "/f";
        final String gPath = usernameHash + "/" + profileUsername + "/g";
        final String hPath = usernameHash + "/" + profileUsername + "/h";

        mFirebaseDatabaseReference.child(fPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                followerCount += dataSnapshot.getChildrenCount();

                mFirebaseDatabaseReference.child(gPath).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        followingCount += dataSnapshot.getChildrenCount();

                        mFirebaseDatabaseReference.child(hPath).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                followerCount += dataSnapshot.getChildrenCount();
                                followingCount += dataSnapshot.getChildrenCount();

                                followerCountTV.setText(Long.toString(followerCount) + "\nFollowers");
                                followingCountTV.setText(Long.toString(followingCount) + "\nFollowing");
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    //get medal counts and points
    private void getGSBP(String userPath){

        final String medalsPath = userPath + "w";
        final String pointsPath = userPath + "p";

        mFirebaseDatabaseReference.child(medalsPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("g")){
                    goldTV.setText(dataSnapshot.child("g").getValue(Integer.class).toString());
                }
                else{
                    goldTV.setText(Integer.toString(0));
                }

                if(dataSnapshot.hasChild("s")){
                    silverTV.setText(dataSnapshot.child("s").getValue(Integer.class).toString());
                }
                else{
                    silverTV.setText(Integer.toString(0));
                }

                if(dataSnapshot.hasChild("b")){
                    bronzeTV.setText(dataSnapshot.child("b").getValue(Integer.class).toString());
                }
                else{
                    bronzeTV.setText(Integer.toString(0));
                }

                mFirebaseDatabaseReference.child(pointsPath).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String strIn;
                        if(dataSnapshot.getValue() != null){
                            strIn = dataSnapshot.getValue(Integer.class).toString() + " influence";
                            pointsTV.setText(strIn);
                        }
                        else{
                            strIn = Integer.toString(0) + " influence";
                            pointsTV.setText(strIn);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void clearProfilePage(){
        profileImageView.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_background));
        goldTV.setText("");
        silverTV.setText("");
        bronzeTV.setText("");
        pointsTV.setText("");
        if(profileUsername != null && !profileUsername.equals(activity.getUsername())){
            commentsORposts = COMMENTS;
        }
        hideProfileImgButtons();
        hideUploadProgressBar();
    }

    private void sendFollowNotification(String fUsername){
        int usernameHash;
        if(fUsername.length() < 5){
            usernameHash = fUsername.hashCode();
        }
        else{
            String hashIn = "" + fUsername.charAt(0) + fUsername.charAt(fUsername.length() - 2) + fUsername.charAt(1) + fUsername.charAt(fUsername.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        String fUserNFPath = Integer.toString(usernameHash) + "/" + fUsername + "/n/f/" + activity.getUsername();
        mFirebaseDatabaseReference.child(fUserNFPath).setValue(System.currentTimeMillis()/1000);    //set value as timestamp as seconds from epoch

    }

    public PostsHistory getPostsHistoryFragment(){
        return postsTab;
    }


}

