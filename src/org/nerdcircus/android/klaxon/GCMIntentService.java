// Google Cloud Messaging Intent Service.
// Handles Intents from GCM, as per developer.android.com/guide/google/gcm/gs.html
package org.nerdcircus.android.klaxon;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;

import org.nerdcircus.android.klaxon.Alert;
import org.nerdcircus.android.klaxon.GcmHelper;
import org.nerdcircus.android.klaxon.Pager;
import org.nerdcircus.android.klaxon.Pager.Pages;

public class GCMIntentService extends GCMBaseIntentService {

    public static String TAG = "GCMIntentService";
    public static String GCM_SENDER_ID = "100533447903";
    public static String MY_TRANSPORT = "gcm";

    // Appengine authentication
    private static final String AUTH_URL = "/_ah/login";
    private static final String AUTH_TOKEN_TYPE = "ah";

    // GCM gateway urls.
    private static final String REGISTER_URL = "/register";
    private static final String UNREGISTER_URL = "/unregister";
    private static final String REPLY_URL = "/reply";


    public GCMIntentService(){
      super(GCM_SENDER_ID); //my project id.
    }

    // This inherits from IntentService, which allows us to handle the reply action here.
    // XXX: Can't do this, because this method is final in GCMBaseIntentService
    //public void onHandleIntent(Intent intent){
    //  if(Pager.REPLY_ACTION == Intent.getAction()){
    //    Log.d(TAG, "** SHOULD REPLY HERE***");
    //  }
    //  else {
    //    super(intent);
    //  }
    //};

    public void onRegistered(Context context, String regId){
      //Called after the device has registered with GCM, so send the regid to our servers.
      GcmHelper gh = new GcmHelper(context);
      gh.register(regId);
    };
    public void onUnregistered(Context context, String regId){
      // Called after device unregisters with gcm, send regid to us, so we can remove it.
      GcmHelper gh = new GcmHelper(context);
      gh.unregister(regId);
    };
    public void onMessage(Context context, Intent intent){
      // Called when a message has been received. Process the received intent.

      Log.d(TAG, "CAUGHT AN INTENT! WHEEEE!");

      if( intent.getAction().equals(Pager.REPLY_ACTION)){
        Log.d(TAG, "Replying!");
        //TODO: actually reply.
        return;
      }

      //check to see if we want to intercept.
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      if( ! prefs.getBoolean("is_oncall", true) ){
          Log.d(TAG, "not oncall. not bothering with incoming c2dm push.");
          return;
      }

      Bundle extras = intent.getExtras();
      if (extras == null)
        return;

      Alert incoming = new Alert();
      if(extras.containsKey("from"))
        incoming.setFrom(extras.getString("from"));
      if(extras.containsKey("subject"))
        incoming.setSubject(extras.getString("subject"));
      if(extras.containsKey("body"))
        incoming.setBody(extras.getString("body"));
      incoming.setTransport(MY_TRANSPORT);

      Uri newpage = context.getContentResolver().insert(Pages.CONTENT_URI, incoming.asContentValues());
      Log.d(TAG, "new message inserted.");
      Intent annoy = new Intent(Pager.PAGE_RECEIVED);
      annoy.setData(newpage);
      context.sendBroadcast(annoy);
      Log.d(TAG, "sent intent " + annoy.toString() );
    };

    public void onError(Context context, String errorId){
      Log.e(TAG, "Encountered a GCM Error: " + errorId);
    };

    // Optional. only override to display a message to the user.
    //public boolean onRecoverableError(Context context, String errorId){};


};
