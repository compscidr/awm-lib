package io.rightmesh.awm;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.vanniktech.rxpermission.Permission;
import com.vanniktech.rxpermission.RealRxPermission;
import com.vanniktech.rxpermission.RxPermission;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class AndroidWirelessStatsCollector {

    private static final String TAG = AndroidWirelessStatsCollector.class.getCanonicalName();
    private Set<StatsCollector> statsCollectors;
    private StatsLogger statsLogger;

    //permission checking code
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 101;
    private RxPermission rxPermission;
    private @NonNull final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private HashMap<String, Permission> permissionResults;
    //all of the permissions that the library needs
    String[] permissions = { Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public AndroidWirelessStatsCollector(Activity activity) {
        Log.i(TAG, "Initializing Android Wireless Stats Collector");

        statsCollectors = new HashSet<>();
        rxPermission = RealRxPermission.getInstance(activity.getApplicationContext());

        //get position so we can log where we are encountering what devices
        GPSStatsCollector gpsStats = new GPSStatsCollector(activity.getApplicationContext());
        statsCollectors.add(gpsStats);

        //bluetooth stats
        BluetoothStatsCollector btStats = new BluetoothStatsCollector(activity.getApplicationContext());
        statsCollectors.add(btStats);

        //WiFi AP stats
        WiFiAPStatsCollector wifiStats = new WiFiAPStatsCollector(activity.getApplicationContext());
        statsCollectors.add(wifiStats);

        //WiFi Direct stats
        WiFiDirectStatsCollector wifiDirectStats = new WiFiDirectStatsCollector(activity.getApplicationContext());
        statsCollectors.add(wifiDirectStats);

        //internet stats
        InternetStatsCollector itStats = new InternetStatsCollector(activity.getApplicationContext());
        statsCollectors.add(itStats);

        //logger to disk and server
        statsLogger = new StatsLogger(activity.getApplicationContext());

        //position stats
        if (!checkPlayServices(activity)) {
            Log.d(TAG, "Missing Google Play Services - GPS likely won't work.");
        }
    }

    public void start() {
        statsLogger.start();

        Log.i(TAG, "Checking permissions");
        permissionResults = new HashMap<>();
        compositeDisposable.add(rxPermission.requestEach(permissions)
                .subscribe(permission -> {
                    Log.i(TAG, permission.name() + " " + permission.state());
                    permissionResults.put(permission.name(), permission);

                    //see if we've received all the permissions yet
                    if(permissionResults.size() == permissions.length) {
                        startStats();
                    }
                }));
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices(Activity activity) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
            }
            return false;
        }
        return true;
    }

    private void startStats() {
        Log.i(TAG, "Starting stats collection");
        for(StatsCollector statsInterface : statsCollectors) {
            statsInterface.setPermissions(permissionResults);

            try {
                statsInterface.start();
            } catch(Exception ex) {
                Log.d(TAG, "Exception starting the stats collection: " + ex.toString());
            }
        }
    }

    public void stop() {
        compositeDisposable.clear();

        Log.i(TAG, "Stopping stats collection");
        for(StatsCollector statsInterface : statsCollectors) {
            statsInterface.stop();
        }

        statsLogger.stop();
    }
}
