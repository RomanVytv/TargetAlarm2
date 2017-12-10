package com.romanvytv.targetalarm2;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;


public class GeofenceHandler {

    private static final long GEO_DURATION = Geofence.NEVER_EXPIRE;
    private static final String GEOFENCE_REQ_ID = "My Geofence";

    // Create a Geofence
    private Geofence createGeofence(double lat, double lng, float radius) {
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    public GeofencingRequest addNewGeofence(double lat, double lng, float radius) {
        return createGeofenceRequest(createGeofence(lat, lng, radius));
    }
}
