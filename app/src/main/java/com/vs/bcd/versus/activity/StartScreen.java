package com.vs.bcd.versus.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class StartScreen extends AppCompatActivity {

    //public static final String EXTRA_MESSAGE = "com.vs.bcd.versus.MESSAGE";


    private CallbackManager callbackManager;
    private LoginButton facebookLoginButton;
    private String esHost = "search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com"; //TODO: eventually we'll be using API gateway at which point we'll remove ES domain info from client side.
    private String esRegion = "us-east-1";
    private String authToken;
    private FirebaseAuth mFirebaseAuth;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private StartScreen thisActivity;
    private ProgressBar nativeProgressbar, facebookProgressbar, googleProgressbar;
    private SignInButton googleLoginButton;
    private GoogleSignInClient mGoogleSignInClient;
    private Button nativeLoginButton;
    private int RC_SIGN_IN = 58;
    private EditText usernameET, pwET;
    private boolean loginThreadRunning = false;
    private AmazonDynamoDBClient ddbClient;
    private DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);
    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);
        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        thisActivity = this;

        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);

        mFirebaseAuth = FirebaseAuth.getInstance();

        callbackManager = CallbackManager.Factory.create();
        facebookProgressbar = findViewById(R.id.facebook_login_progress_bar);
        facebookLoginButton = findViewById(R.id.facebook_login_button);
        facebookLoginButton.setOnClickListener(new View.OnClickListener() { //This is an external click listener. Internal click listener handles the login.
            @Override
            public void onClick(View view) {
                resetLoginButtons();
                facebookLoginButton.setVisibility(View.INVISIBLE);
                facebookProgressbar.setVisibility(View.VISIBLE);
            }
        });


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken("688623904224-mh3oeo8ega5uhlufua0f1rtrncqoj8k7.apps.googleusercontent.com").build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleProgressbar = findViewById(R.id.google_login_progress_bar);
        googleLoginButton = findViewById(R.id.google_login_button);
        googleLoginButton.setSize(SignInButton.SIZE_WIDE);
        googleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetLoginButtons();
                googleLoginButton.setVisibility(View.INVISIBLE);
                googleProgressbar.setVisibility(View.VISIBLE);
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
        //googleLoginButton.setColorScheme(SignInButton.COLOR_DARK);

        //facebookLoginButton.setReadPermissions("public_profile");
        // Callback registration
        facebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                authToken = loginResult.getAccessToken().getToken();

                //TODO: display a progress bar while the GraphRequest and ES Query are working their magic

                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.v("facebookLogin", response.toString());

                                try{
                                    String name = object.getString("name");
                                    String fname = object.getString("first_name");
                                    String lname = object.getString("last_name");
                                    final String authID = object.getString("id") + "&";
                                    final String firstname, lastname;
                                    if(fname != null && !fname.isEmpty()){
                                        if(lname != null && !lname.isEmpty()){
                                            firstname = fname;
                                            lastname = lname;
                                        }
                                        else{
                                            firstname = fname;
                                            lastname = " "; //TODO: once we move on from ddb, this can be empty string instead of a space character
                                        }
                                    }
                                    else if(lname != null && !lname.isEmpty()){
                                        firstname = " ";
                                        lastname = lname;
                                    }
                                    else if(name != null && !name.isEmpty()){
                                        firstname = name;
                                        lastname = " ";
                                    }
                                    else{
                                        firstname = "N/A";
                                        lastname = " ";
                                    }

                                    Runnable runnable = new Runnable() {
                                        public void run() {
                                            logInOrSignUpUser(authID, firstname, lastname);
                                        }
                                    };
                                    Thread mythread = new Thread(runnable);
                                    mythread.start();

                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "first_name,last_name,name");
                request.setParameters(parameters);
                request.executeAsync();

                Log.d("facebookLogin", "login success.");

                //LoginManager.getInstance().logOut();
            }

            @Override
            public void onCancel() {
                // App code
                Log.d("facebookLogin", "fb login was cancelled.");
                resetLoginButtons();
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Log.d("facebookLogin", "fb login error.");
                resetLoginButtons();
            }
        });
    /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    */


        usernameET = findViewById(R.id.editTextUsernameIN);
        pwET = findViewById(R.id.editTextPWIN);
        nativeProgressbar = findViewById(R.id.native_login_progress_bar);
        nativeLoginButton = findViewById(R.id.native_login_button);

        //check if facebook user logged in first, and also clear other providers including Cognito
        resetLoginButtons();


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess()){
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
            }
            else{
                resetLoginButtons();
            }
        }
        else{
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {

            final String firstname, lastname;
            final GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            authToken = account.getIdToken();

            if(account.getGivenName() == null || account.getGivenName().isEmpty()){
                firstname = " ";
            }
            else{
                firstname = account.getGivenName();
            }
            if(account.getFamilyName() == null || account.getFamilyName().isEmpty()) {
                lastname = " ";
            }
            else{
                lastname = account.getFamilyName();
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    logInOrSignUpUser(account.getId(), firstname, lastname);
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();

            // Signed in successfully, show authenticated UI.
            //updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("googleLogin", "signInResult:failed code=" + e.getStatusCode());
            //updateUI(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void signUpPressed(View view){
        Intent intent = new Intent(this, SignUp.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        resetLoginButtons();
    }

    @Override
    protected void onStart(){
        super.onStart();
        displayLoginProgressbar(false);
    }

    private void displayLoginProgressbar(boolean set){
        if(set){
            Log.d("loginpb", "true");
            nativeLoginButton.setClickable(false);
            nativeLoginButton.setVisibility(View.INVISIBLE);
            nativeLoginButton.setEnabled(false);

            nativeProgressbar.setEnabled(true);
            nativeProgressbar.setVisibility(View.VISIBLE);
        }
        else{
            nativeProgressbar.setEnabled(false);
            nativeProgressbar.setVisibility(View.INVISIBLE);

            nativeLoginButton.setEnabled(true);
            nativeLoginButton.setVisibility(View.VISIBLE);
            nativeLoginButton.setClickable(true);
        }

    }

    public void logInPressed(View view){
        if(usernameET.getText().toString().trim().isEmpty()){
            if(mToast != null){
                mToast.cancel();
            }
            mToast = Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT);
            mToast.show();
            return;
        }
        if(pwET.getText().toString().trim().isEmpty()){
            if(mToast != null){
                mToast.cancel();
            }
            mToast = Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT);
            mToast.show();
            return;
        }
        resetLoginButtons();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow( getCurrentFocus().getWindowToken(), 0);
        //TODO: better way to prevent multiple login submission from tapping the button rapidly multiple times?

        if(!loginThreadRunning){
            loginThreadRunning = true;
            displayLoginProgressbar(true);

            final String usernameIn = usernameET.getText().toString();

            //TODO: form validation here
            try{
                mFirebaseAuth.signInWithEmailAndPassword(usernameIn + "@versusbcd.com", pwET.getText().toString())
                        .addOnCompleteListener(StartScreen.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d("authsuccess", "aye success");
                                    FirebaseUser firebaseUser= mFirebaseAuth.getCurrentUser();

                                    if(firebaseUser != null){

                                        firebaseUser.getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                                            @Override
                                            public void onSuccess(GetTokenResult getTokenResult) {
                                                Map<String, String> logins = new HashMap<>();
                                                final String token = getTokenResult.getToken();
                                                logins.put("securetoken.google.com/bcd-versus", token);
                                                credentialsProvider.setLogins(logins);


                                                    Runnable runnable = new Runnable() {
                                                        public void run() {
                                                            try{
                                                                credentialsProvider.refresh();
                                                                final User user = mapper.load(User.class, usernameIn); //TODO: replace with ES user GET

                                                                thisActivity.runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        SessionManager sessionManager = new SessionManager(thisActivity);
                                                                        sessionManager.createLoginSession(user);    //store login session data in Shared Preferences

                                                                        Intent intent = new Intent(thisActivity, MainContainer.class);
                                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                        intent.putExtra("oitk", token);
                                                                        loginThreadRunning = false;
                                                                        startActivity(intent);  //go on to the next activity, MainContainer
                                                                        overridePendingTransition(0, 0);
                                                                    }
                                                                });
                                                            }catch (NotAuthorizedException e){
                                                                thisActivity.runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        resetLoginButtons();
                                                                        if(mToast != null){
                                                                            mToast.cancel();
                                                                        }
                                                                        mToast = Toast.makeText(thisActivity, "Something went wrong. Please try again.", Toast.LENGTH_SHORT);
                                                                    }
                                                                });
                                                            }

                                                        }
                                                    };
                                                    Thread mythread = new Thread(runnable);
                                                    mythread.start();


                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                displayLoginProgressbar(false);
                                                loginThreadRunning = false;
                                                Toast.makeText(thisActivity, "There was a problem logging in. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                    else{
                                        displayLoginProgressbar(false);
                                        loginThreadRunning = false;
                                        Toast.makeText(thisActivity, "There was a problem logging in. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else {
                                    displayLoginProgressbar(false);
                                    loginThreadRunning = false;
                                    Toast.makeText(StartScreen.this, "Check your username or password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }catch (Exception e){
                displayLoginProgressbar(false);
                loginThreadRunning = false;
                Toast.makeText(StartScreen.this, "Check your username or password", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }


    private void logInOrSignUpUser(String authID, String firstname, String lastname){ //for facebook login and google login. Logs the user in if user is already registered with the app, otherwise it registers the user with the app

        String query = "/user/_search";
        String payload = "{\"size\":1,\"query\":{\"match\":{\"ai\":\""+authID+"\"}}}";


        String url = "https://" + esHost + query;

        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", esHost);

        AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                .regionName(esRegion)
                .serviceName("es") // es - elastic search. use your service name
                .httpMethodName("POST") //GET, PUT, POST, DELETE, etc...
                .canonicalURI(query) //end point
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .payload(payload) // payload if any
                .debug() // turn on the debug mode
                .build();

        HttpPost httpPost = new HttpPost(url);
        StringEntity requestEntity = new StringEntity(payload, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);

        /* Get header calculated for request */
        Map<String, String> header = aWSV4Auth.getHeaders();
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();

            /* Attach header in your request */
            /* Simple get request */

            httpPost.addHeader(key, value);
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

            String strResponse = httpClient.execute(httpPost, responseHandler);

            JSONObject obj = new JSONObject(strResponse);
            JSONArray hits = obj.getJSONObject("hits").getJSONArray("hits");
            //Log.d("idformat", hits.getJSONObject(0).getString("_id"));
            if(hits.length() == 0){
                //this is a first time login, so register the user
                //take user to username input & birthday input page to finish registration
                //once the registration is complete,
                //authenticate with firebase, then setLogins for cognito with firebase token, then create client session with SessionManager

                Intent intent = new Intent(this, AuthSignUp.class);
                intent.putExtra("firstname", firstname);
                intent.putExtra("lastname", lastname);
                intent.putExtra("authid", authID);
                intent.putExtra("token", authToken);
                startActivity(intent);
                overridePendingTransition(0, 0);
                resetLoginButtonsUIOnly();
            }
            else{
                JSONObject item = hits.getJSONObject(0);
                //construct user object from the response item and sign in,
                // creating session with SessionManager and navigating to MainContainer
                final User user = new User(item.getJSONObject("_source"), item.getString("_id"));
                Log.d("facebookLogin", "user exists");
                //authenticate with firebase, then setLogins for cognito with firebase token, then create client session with SessionManager

                AuthCredential credential;
                if(authID.charAt(authID.length()-1) == '&'){ //we append facebook authIDs with an '&'
                    credential = FacebookAuthProvider.getCredential(authToken);
                }
                else{
                    credential = GoogleAuthProvider.getCredential(authToken, null);
                }

                mFirebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d("facebookLogin", "signInWithCredential:success");
                                    FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();

                                    if(firebaseUser != null){
                                        mFirebaseAuth.getCurrentUser().getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                                            @Override
                                            public void onSuccess(final GetTokenResult getTokenResult) {
                                                Map<String, String> logins = new HashMap<>();
                                                logins.put("securetoken.google.com/bcd-versus", getTokenResult.getToken());
                                                credentialsProvider.setLogins(logins);

                                                Runnable runnable = new Runnable() {
                                                    public void run() {
                                                        try {
                                                            credentialsProvider.refresh();

                                                            thisActivity.runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    SessionManager sessionManager = new SessionManager(thisActivity);
                                                                    sessionManager.createLoginSession(user);
                                                                    Intent intent = new Intent(thisActivity, MainContainer.class);
                                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);   //clears back stack for navigation
                                                                    intent.putExtra("oitk", getTokenResult.getToken());
                                                                    startActivity(intent);
                                                                    overridePendingTransition(0, 0);
                                                                }
                                                            });
                                                        }catch (NotAuthorizedException e){
                                                            thisActivity.runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    resetLoginButtons();
                                                                    if(mToast != null){
                                                                        mToast.cancel();
                                                                    }
                                                                    mToast = Toast.makeText(thisActivity, "Something went wrong. Please try again.", Toast.LENGTH_SHORT);
                                                                    mToast.show();
                                                                }
                                                            });
                                                        }
                                                    }
                                                };
                                                Thread mythread = new Thread(runnable);
                                                mythread.start();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                thisActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        // If sign in fails, display a message to the user.
                                                        Toast.makeText(thisActivity, "Authentication failed.",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        });
                                    }
                                    else {
                                        thisActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // If sign in fails, display a message to the user.
                                                Toast.makeText(thisActivity, "Authentication failed.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                } else {
                                    thisActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // If sign in fails, display a message to the user.
                                            Toast.makeText(thisActivity, "Authentication failed.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        });

            }
        } catch (Exception e) {
            e.printStackTrace();
            //TODO: remove progress bar, pop a toast saying something went wrong, try again or check network connection
        }
    }

    private void resetLoginButtons(){
        //TODO: check if facebook user logged in first, and also clear other providers including Cognito
        Log.d("facebookLogin", "logging out facebook user");
        LoginManager.getInstance().logOut();
        facebookLoginButton.setVisibility(View.VISIBLE);
        facebookProgressbar.setVisibility(View.INVISIBLE);

        Log.d("googleLogin", "logging out google user");
        mGoogleSignInClient.signOut();
        googleLoginButton.setVisibility(View.VISIBLE);
        googleProgressbar.setVisibility(View.INVISIBLE);

        displayLoginProgressbar(false);
        loginThreadRunning = false;
    }

    private void resetLoginButtonsUIOnly(){
        facebookLoginButton.setVisibility(View.VISIBLE);
        facebookProgressbar.setVisibility(View.INVISIBLE);

        googleLoginButton.setVisibility(View.VISIBLE);
        googleProgressbar.setVisibility(View.INVISIBLE);

        displayLoginProgressbar(false);
    }

}
