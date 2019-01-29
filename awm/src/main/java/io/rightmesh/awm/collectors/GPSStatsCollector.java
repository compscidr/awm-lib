package io.rightmesh.awm.collectors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import io.rightmesh.awm.AndroidWirelessStatsCollector;
import io.rightmesh.awm.stats.GPSStats;

/**
 * https://stackoverflow.com/questions/28535703/best-way-to-get-user-gps-location-in-background-in-android
 */
public class GPSStatsCollector extends StatsCollector {

    private Context context;
    private static final String TAG = AndroidWirelessStatsCollector.class.getCanonicalName();

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;

    public GPSStatsCollector(Context context) {

        this.context = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(this.locationRequest);
        locationSettingsRequest = builder.build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult); // why? this. is. retarded. Android.
                Location currentLocation = locationResult.getLastLocation();

                Log.i(TAG, "Location Callback results: " + currentLocation.getLatitude()
                        + " " + currentLocation.getLongitude());

                eventBus.post(new GPSStats(currentLocation.getLongitude(), currentLocation.getLatitude()));
            }
        };
    }

    @Override
    public void start() throws Exception {
        if(Build.VERSION.SDK_INT >= 23) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                throw new Exception("No permission for location, cannot get GPS positions");
            }
        }

        mFusedLocationClient.requestLocationUpdates(this.locationRequest,
                this.locationCallback, Looper.myLooper());
    }

    @Override
    public void stop() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
