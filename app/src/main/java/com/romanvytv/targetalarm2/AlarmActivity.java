package com.romanvytv.targetalarm2;

import android.app.Activity;
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

    Button stopBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_activity_layout);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wl.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] vibroPattern = {0,700,700};

        vibrator.vibrate(vibroPattern,0);

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        final Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
        ringtone.play();

        stopBtn = (Button) findViewById(R.id.buttonStop);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vibrator.cancel();
                ringtone.stop();
                finish();
            }
        };
        stopBtn.setOnClickListener(listener);

        //NotificationManager notifier = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //notifier.cancel(MainActivity.NOTIFY_ID);
        MainActivity.alarmRunned();
        stopService(new Intent(this,GeofenceService.class));
    }
}
