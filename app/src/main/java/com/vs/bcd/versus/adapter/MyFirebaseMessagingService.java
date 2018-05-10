/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vs.bcd.versus.adapter;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String INTENT_FILTER = "INTENT_FILTER";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Handle data payload of FCM messages.
        //Log.d("MyFMService", "FCM Message Id: " + remoteMessage.getMessageId());
        //Log.d("MyFMService", "FCM Notification Message: " + remoteMessage.getNotification());
        //Log.d("MyFMService", "FCM Data Message: " + remoteMessage.getData());
        if(remoteMessage.getData().get("type").equals("m")){
            Intent intent = new Intent(INTENT_FILTER);
            //intent.putExtra("roomNum", remoteMessage.getData().get("room_id"));
            sendBroadcast(intent);
        }

    }
}