package com.romanvytv.targetalarm2;

import android.app.IntentService;
import android.content.Intent;


public class GeofenceTransitionsIntentService extends IntentService {

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }

    protected void onHandleIntent(Intent intent) {

        Intent launchAlarmIntent = new Intent(this, AlarmActivity.class);
        launchAlarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(launchAlarmIntent);
    }
}