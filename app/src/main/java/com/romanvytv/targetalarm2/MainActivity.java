package com.romanvytv.targetalarm2;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import static com.romanvytv.targetalarm2.R.id.map;


public class MainActivity extends FragmentActivity
        implements OnMapReadyCallback,GoogleMap.OnMapClickListener {

    private static SharedPreferences myTarget;
    private GoogleMap myMap;
    private LatLng myLatLng = null;
    private static double myLongtitude = 48.922778;
    private static double myLatitiude = 24.710556;
    private static int radius = 50;

    private static Target target;
    private static Switch mySwitch;
    private static NotificationManager notifier;
    public static final int NOTIFY_ID = 101;
    private static String jsonStrTargets = "";
    boolean flag;
    public static GoogleApiClient myNewGoogleApiClient;

    public static final String TARGET_DATA = "targets_data";
    public static final String TARGET_DATA_STR = "targets_data_str";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        //ініціалізація
        myTarget =  getSharedPreferences(TARGET_DATA, Context.MODE_PRIVATE);
        notifier = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mySwitch = (Switch) findViewById(R.id.switch1);


        flag = myTarget.contains(TARGET_DATA_STR);
        // установка радіуса
        final TextView textViewKM = (TextView) findViewById(R.id.textViewKM);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);

        //максимальний радіус
        seekBar.setMax(5000);
        textViewKM.setText("0.05 km");


        target = new Target();
        initTarget();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progress1 = 0;

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //мінімальний радіус
                if (progress1 < 50) {
                    progress1 = 50;
                    seekBar.setProgress(progress1);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                progress1 = progress;
                double progress2 = progress1;
                textViewKM.setText(String.format("%.2f km", progress2 / 1000));
                radius = (int) progress2;
                if (myLatLng != null) {
                    updateMarker(myLatLng);
                    updateJsonTarget(myLatitiude,myLongtitude,radius);
                }
            }
        });

        mySwitch.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateJsonTarget(target.getLatitude(),target.getLongtitude(),target.getRadius());
                init();
                if(isChecked){
                    notif();
                    startGeofencing();
                }else {
                    notifier.cancel(NOTIFY_ID);
                    stopGeofencing();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {

        map.setMyLocationEnabled(true);
        LatLng usersLoc = new LatLng(48.932068, 24.698659);
        map.moveCamera(CameraUpdateFactory.newLatLng(usersLoc));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        myMap = map;
        myMap.setOnMapClickListener(this);

        // встановлення маркеру  цілі
        myLatLng = new LatLng(target.getLatitude(), target.getLongtitude());
        myMap.addMarker(new MarkerOptions().position(myLatLng).title("Your current target"));
        myMap.addCircle(new CircleOptions().center(myLatLng)
                .radius(radius).strokeColor(R.color.colorPrimary).strokeWidth(3));
        myMap.moveCamera(CameraUpdateFactory.zoomTo(11));
        myMap.moveCamera(CameraUpdateFactory.newLatLng(myLatLng));
    }

    public void updateMarker(LatLng latLng) {
        myMap.clear();
        myMap.addMarker(new MarkerOptions().position(latLng).title("Your current target"));
        myMap.addCircle(new CircleOptions().center(latLng).radius(radius).strokeColor(R.color.colorPrimary).strokeWidth(3));
    }

    @Override
    public void onMapClick(LatLng latLng) {
        //оновлення маркеру при кліку по карті
        myLatLng = latLng;
        myLatitiude = latLng.latitude;
        myLongtitude = latLng.longitude;
        updateMarker(latLng);
        updateJsonTarget(latLng.latitude,latLng.longitude,radius);
    }

    //серіалізація цілі
    public static void updateJsonTarget(double lat, double lng, int radius) {
        target.setLatitude(lat);
        target.setLongtitude(lng);
        target.setRadius(radius);
        target.setRunning(mySwitch.isChecked());
        jsonStrTargets = new Gson().toJson(target);
        SharedPreferences.Editor editor = MainActivity.myTarget.edit();
        editor.putString(TARGET_DATA_STR, jsonStrTargets);
        editor.apply();
    }

    public void init(){
        if(flag){
            target = new Gson().fromJson(myTarget.getString(TARGET_DATA_STR,""),
                    new TypeToken<Target>(){}.getType() );
        }
    }

    //десеріалізація цілі
    private void initTarget(){
        init();
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress(target.getRadius());
        TextView textViewKM = (TextView) findViewById(R.id.textViewKM);
        textViewKM.setText(String.format("%.2f km", (double)target.getRadius() / 1000));
        myLatitiude = target.getLatitude();
        myLongtitude = target.getLongtitude();
        radius = target.getRadius();
        if(target.isRunning()){
            mySwitch.toggle();
        }
    }

    private void notif(){
        final Notification actionBarNotification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.icon_48dp)
                .setContentTitle("Big brother watching you!")
                .build();
        actionBarNotification.flags |= Notification.FLAG_NO_CLEAR
                | Notification.FLAG_ONGOING_EVENT;
        notifier.notify(NOTIFY_ID,actionBarNotification);
    }

    private void startGeofencing(){
        Intent intent = new Intent(this,GeofenceService.class);
        startService(intent);
    }

    private void stopGeofencing() {
        if(GeofenceService.getGoogleApiClient() != null)
            GeofenceService.getGoogleApiClient().disconnect();

    }

    public static Target getTarget() {
        return target;
    }

    public static int getRadius() {
        return radius;
    }

    public static double getMyLongtitude() {
        return myLongtitude;
    }

    public static double getMyLatitiude() {
        return myLatitiude;
    }

    public static void alarmRunned(){
        notifier.cancel(NOTIFY_ID);
        mySwitch.toggle();
    }
}


