package com.romanvytv.targetalarm2;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
        implements GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    public static final String TARGET_DATA = "TARGET_DATA";
    public static final String TARGET_DATA_STR = "TARGET_DATA_STR";
    private static final int MAX_RADIUS_METRES = 5000;
    private static final int MIN_RADIUS_METRES = 50;

    private SharedPreferences sharedPreferences;
    private GoogleMap googleMap;
    private LatLng myLatLng;

    private Target target;

    private NotificationManager notifier;
    public static final int NOTIFY_ID = 101;

    public Intent intentLaunch;
    public PendingIntent geofencePendingIntentLaunch;

    private IntentFilter filter;
    private StopReceiver receiver;

    private GoogleApiClient googleApiClient;
    private GeofenceHandler geofenceHandler;

    private Switch mySwitch;
    private TextView textViewKM;
    private SeekBar seekBar;

    private float[] distanceToTarget = new float[2];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_layout);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(map);
        mapFragment.getMapAsync(new MapReadyCallback());

        sharedPreferences = getSharedPreferences(TARGET_DATA, Context.MODE_PRIVATE);
        notifier = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        geofenceHandler = new GeofenceHandler();

        initIntents();
        initApiClient();
        initViews();
        initTargetInfo();

        registerReceiver(receiver, filter);
    }

    private void initIntents() {
        intentLaunch = new Intent(this, GeofenceTransitionsIntentService.class);
        intentLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        geofencePendingIntentLaunch =
                PendingIntent.getService(this, 0, intentLaunch, PendingIntent.FLAG_UPDATE_CURRENT);

        filter = new IntentFilter(AlarmActivity.STOP_GEOFENCING);
        receiver = new StopReceiver();
    }

    private void initApiClient() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new ConnectionCallbacks())
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void initViews() {
        textViewKM = findViewById(R.id.textViewKM);
        seekBar = findViewById(R.id.seekBar);
        mySwitch = findViewById(R.id.switch1);

        //максимальний радіус (м)
        seekBar.setMax(MAX_RADIUS_METRES);
        textViewKM.setText(R.string.min_radius);

        seekBar.setOnSeekBarChangeListener(new SeekBarListener());
        mySwitch.setOnCheckedChangeListener(new SwitchCheckedListener());

    }

    private void initTargetInfo() {
        target = getTargetFromPrefs();
        seekBar.setProgress(target.getRadius());
        textViewKM.setText(getRadiusInKm(target.getRadius()));
        mySwitch.setChecked(target.isEnabled());
    }

    private final class SeekBarListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //мінімальний радіус
            if (seekBar.getProgress() < MIN_RADIUS_METRES) {
                seekBar.setProgress(MIN_RADIUS_METRES);
                saveLocation(target.getLatitude(), target.getLongtitude(), MIN_RADIUS_METRES);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            textViewKM.setText(getRadiusInKm(progress));
            target.setRadius(progress);
            if (myLatLng != null) {
                updateMarker(myLatLng);
                saveLocation(target.getLatitude(), target.getLongtitude(), target.getRadius());
            }
        }
    }

    private final class SwitchCheckedListener implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            saveLocation(target.getLatitude(), target.getLongtitude(), target.getRadius());
            if (isChecked) {
                notif();
                startGeofencing();
            } else {
                notifier.cancel(NOTIFY_ID);
                stopGeofencing();
            }
        }
    }


    private final class MapReadyCallback implements OnMapReadyCallback {

        @Override
        public void onMapReady(GoogleMap map) {

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            map.setMyLocationEnabled(true);
            googleMap = map;
            googleMap.setOnMapClickListener(new OnMapClickListener());

            // встановлення маркеру  цілі
            myLatLng = new LatLng(target.getLatitude(), target.getLongtitude());
            updateMarker(myLatLng);
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(11));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(myLatLng));
        }
    }

    private final class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
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
            final int UPDATE_INTERVAL = 1000;
            final int FASTEST_INTERVAL = 900;

            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(UPDATE_INTERVAL)
                    .setFastestInterval(FASTEST_INTERVAL);

            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(), target.getLatitude(), target.getLongtitude(), distanceToTarget);
                    notif();
                }
            });


            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    geofenceHandler.addNewGeofence(
                            target.getLatitude(),
                            target.getLongtitude(),
                            target.getRadius()),
                    geofencePendingIntentLaunch).setResultCallback(MainActivity.this);

        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    }

    private final class OnMapClickListener implements GoogleMap.OnMapClickListener {

        @Override
        public void onMapClick(LatLng latLng) {
            //оновлення маркеру при кліку по карті
            myLatLng = latLng;
            target.setLatitude(latLng.latitude);
            target.setLongtitude(latLng.longitude);
            if (mySwitch.isChecked())
                mySwitch.toggle();
            updateMarker(latLng);
            saveLocation(latLng.latitude, latLng.longitude, target.getRadius());
        }
    }

    public void updateMarker(LatLng latLng) {
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(latLng).title("Your current target"));
        googleMap.addCircle(new CircleOptions().center(latLng).radius(target.getRadius())
                .strokeColor(R.color.colorPrimary).strokeWidth(3));
    }

    //серіалізація цілі
    public void saveLocation(double lat, double lng, int radius) {
        target.setLatitude(lat);
        target.setLongtitude(lng);
        target.setRadius(radius);
        target.setEnabled(mySwitch.isChecked());
        String jsonStrTargets = new Gson().toJson(target);
        sharedPreferences.edit().putString(TARGET_DATA_STR, jsonStrTargets).apply();
    }

    //десеріалізація цілі
    public Target getTargetFromPrefs() {
        if (!sharedPreferences.contains(TARGET_DATA_STR))
            return new Target();

        target = new Gson().fromJson(sharedPreferences.getString(TARGET_DATA_STR, ""),
                new TypeToken<Target>() {
                }.getType());
        return target;
    }

    private void notif() {
        Notification actionBarNotification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.icon_48dp)
                .setContentTitle("Geofencing enable!")
                .setContentText("Distance to target - " + getRadiusInKm((int) getDistanceToTarget()))
                .build();
        actionBarNotification.flags |= Notification.FLAG_NO_CLEAR
                | Notification.FLAG_ONGOING_EVENT;
        notifier.notify(NOTIFY_ID, actionBarNotification);
    }

    private void startGeofencing() {
        googleApiClient.connect();
    }

    private void stopGeofencing() {
        if (googleApiClient != null) {
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient, geofencePendingIntentLaunch);
            googleApiClient.disconnect();
            target.setEnabled(false);
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(@NonNull Status status) {
    }

    private String getRadiusInKm(int radius) {
        return String.valueOf((double) radius / 1000) + " km";
    }

    private class StopReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mySwitch.toggle();
            saveLocation(target.getLatitude(), target.getLongtitude(), target.getRadius());
        }
    }

    public float getDistanceToTarget() {
        return distanceToTarget[0] - target.getRadius();
    }
}


