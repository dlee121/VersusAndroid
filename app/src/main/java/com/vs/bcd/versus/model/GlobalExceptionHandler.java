package com.vs.bcd.versus.model;

import android.util.Log;

import com.amazonaws.mobileconnectors.apigateway.ApiClientException;
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException;
import com.vs.bcd.versus.activity.MainContainer;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    public static final String EXTRA_MY_EXCEPTION_HANDLER = "EXTRA_MY_EXCEPTION_HANDLER";
    private final MainContainer context;
    private final Thread.UncaughtExceptionHandler rootHandler;

    public GlobalExceptionHandler(MainContainer context) {
        this.context = context;
        // we should store the current exception handler -- to invoke it for all not handled exceptions ...
        rootHandler = Thread.getDefaultUncaughtExceptionHandler();
        // we replace the exception handler now with us -- we will properly dispatch the exceptions ...
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable e) {
        e.printStackTrace();
        if (e instanceof ApiClientException) {
            context.handleNotAuthorizedException();
            // make sure we kill it to prevent hanging
            //android.os.Process.killProcess(android.os.Process.myPid());
            //System.exit(0);
        }
        else if(e instanceof NotAuthorizedException){
            context.handleNotAuthorizedException();
            // make sure we kill it to prevent hanging
            //android.os.Process.killProcess(android.os.Process.myPid());
            //System.exit(0);
        }
        else {
            rootHandler.uncaughtException(thread, e);
        }
    }
}