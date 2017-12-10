package com.romanvytv.targetalarm2;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;


public class AlarmActivity extends Activity {

    public static final String STOP_GEOFENCING = "STOP_GEOFENCING";

    private long[] vibroPattern = {0, 700, 700};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wakeUpScreen();

        setContentView(R.layout.alarm_activity_layout);

        final Vibrator vibro = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        final Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);

        ringtone.play();
        vibro.vibrate(vibroPattern, 0);

        Button stopBtn = findViewById(R.id.buttonStop);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vibro.cancel();
                ringtone.stop();
                finish();
            }
        });

        NotificationManager notifier = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifier.cancel(MainActivity.NOTIFY_ID);
        stopService(new Intent(this, GeofenceTransitionsIntentService.class));
        sendBroadcast(new Intent("STOP_GEOFENCING"));
    }

    private void wakeUpScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");
        wl.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }
}
