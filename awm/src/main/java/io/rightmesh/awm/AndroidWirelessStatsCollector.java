package io.rightmesh.awm;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.anadeainc.rxbus.Bus;
import com.anadeainc.rxbus.BusProvider;
import com.anadeainc.rxbus.Subscribe;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.vanniktech.rxpermission.Permission;
import com.vanniktech.rxpermission.RealRxPermission;
import com.vanniktech.rxpermission.RxPermission;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.rightmesh.awm.collectors.BluetoothStatsCollector;
import io.rightmesh.awm.collectors.GPSStatsCollector;
import io.rightmesh.awm.collectors.InternetStatsCollector;
import io.rightmesh.awm.collectors.StatsCollector;
import io.rightmesh.awm.collectors.WiFiAPStatsCollector;
import io.rightmesh.awm.collectors.WiFiDirectStatsCollector;
import io.rightmesh.awm.loggers.DiskLogger;
import io.rightmesh.awm.loggers.NetworkLogger;
import io.rightmesh.awm.loggers.StatsLogger;
import io.rightmesh.awm.stats.BluetoothStats;
import io.rightmesh.awm.stats.GPSStats;
import io.rightmesh.awm.stats.NetworkStat;
import io.rightmesh.awm.stats.WiFiStats;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class AndroidWirelessStatsCollector {

    private boolean uploadImmediately = false;
    private static final String SHARED_PREF_FILE = "uuid.dat";
    private static final String TAG = AndroidWirelessStatsCollector.class.getCanonicalName();
    private Set<StatsCollector> statsCollectors;
    private Set<StatsLogger> statsLoggers;
    private Bus eventBus = BusProvider.getInstance();

    //permission checking code
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 101;
    private RxPermission rxPermission;
    private @NonNull final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private HashMap<String, Permission> permissionResults;

    //all of the permissions that the library needs
    private String[] permissions = { Manifest.permission.BLUETOOTH,
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

    private NetworkLogger networkLogger;
    private DiskLogger diskLogger;

    private ScheduledExecutorService scheduleTaskExecutor;
    private ObservingDevice thisDevice;
    private SharedPreferences sharedPreferences;

    public AndroidWirelessStatsCollector(Activity activity, boolean uploadImmediately) {
        Log.i(TAG, "Initializing Android Wireless Stats Collector");

        this.uploadImmediately = uploadImmediately;

        sharedPreferences = activity.getApplicationContext()
                .getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);

        scheduleTaskExecutor = Executors.newScheduledThreadPool(5);

        UUID uuid;
        String uuidString = sharedPreferences.getString("uuid", null);
        if(uuidString == null) {
            uuid = UUID.randomUUID();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("uuid", uuid.toString());
            editor.apply();
        } else {
            uuid = UUID.fromString(uuidString);
        }

        //detect Android OS version
        String OS = Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.SDK_INT + " "
                + Build.VERSION.RELEASE;
        thisDevice = new ObservingDevice(uuid, OS);

        statsCollectors = new HashSet<>();
        statsLoggers = new HashSet<>();

        rxPermission = RealRxPermission.getInstance(activity.getApplicationContext());

        GPSStatsCollector gpsStats = new GPSStatsCollector(activity.getApplicationContext());
        statsCollectors.add(gpsStats);

        BluetoothStatsCollector btStats = new BluetoothStatsCollector(activity.getApplicationContext());
        statsCollectors.add(btStats);

        WiFiAPStatsCollector wifiStats = new WiFiAPStatsCollector(activity.getApplicationContext());
        statsCollectors.add(wifiStats);

        WiFiDirectStatsCollector wifiDirectStats = new WiFiDirectStatsCollector(activity.getApplicationContext());
        statsCollectors.add(wifiDirectStats);

        InternetStatsCollector itStats = new InternetStatsCollector(activity.getApplicationContext());
        statsCollectors.add(itStats);

        networkLogger = new NetworkLogger();
        statsLoggers.add(networkLogger);

        diskLogger = new DiskLogger(activity.getApplicationContext());
        statsLoggers.add(diskLogger);

        if (!checkPlayServices(activity)) {
            Log.d(TAG, "Missing Google Play Services - GPS likely won't work.");
        }
    }

    public void start() {
        Log.i(TAG, "Checking permissions");
        permissionResults = new HashMap<>();
        compositeDisposable.add(rxPermission.requestEach(permissions)
                .subscribe(permission -> {
                    Log.i(TAG, permission.name() + " " + permission.state());
                    permissionResults.put(permission.name(), permission);

                    //see if we've received all the permissions yet
                    if(permissionResults.size() == permissions.length) {
                        startLoggers();
                        startStats();
                    }
                }));
        eventBus.register(this);
        //scheduleTaskExecutor.scheduleAtFixedRate(
        //        this::uploadDisk, 0, 10, TimeUnit.SECONDS);
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

    private void startLoggers() {
        Log.i(TAG, "Starting stats loggers");
        for(StatsLogger statsLogger : statsLoggers) {
            try {
                statsLogger.start();
            } catch(Exception ex) {
                Log.e(TAG, "Exception starting the stats logger: " + ex.toString());
                ex.printStackTrace();
            }
        }
    }

    private void startStats() {
        Log.i(TAG, "Starting stats collection");
        for(StatsCollector statsInterface : statsCollectors) {
            statsInterface.setPermissions(permissionResults);
            try {
                statsInterface.start();
            } catch(Exception ex) {
                Log.d(TAG, "Exception starting the stats collection: " + ex.toString());
                ex.printStackTrace();
            }
        }
    }

    public void stop() {
        compositeDisposable.clear();

        Log.i(TAG, "Stopping stats collection");
        for(StatsCollector statsInterface : statsCollectors) {
            statsInterface.stop();
        }

        for(StatsLogger statsLogger: statsLoggers) {
            statsLogger.stop();
        }
    }

    @Subscribe
    public void updateNetworkStats(NetworkStat networkStat) {
        Log.d(TAG, "UPDATE NETWORK STATS");
        if(networkStat instanceof WiFiStats) {
            thisDevice.setWifiMac(networkStat.getMac());
        } else if(networkStat instanceof BluetoothStats) {
            thisDevice.setBluetoothMac(networkStat.getMac());
        }

        try {
            if (uploadImmediately) {
                networkLogger.log(networkStat, thisDevice);
            } else {
                diskLogger.log(networkStat, thisDevice);
            }
        } catch(Exception ex) {
            try {
                diskLogger.log(networkStat, thisDevice);
            } catch(Exception ex2) {
                Log.e(TAG, "Failed to log to network and disk. Results lost: "
                        + ex2.toString());
                ex2.printStackTrace();
            }
        }
    }

    @Subscribe
    public void updateGPS(GPSStats gpsStats) {
        thisDevice.updatePosition(gpsStats);
    }

    private void uploadDisk() {
        String jsondata;
        try {
            jsondata = diskLogger.getPendingLogs();
        } catch(Exception ex) {
            Log.d(TAG, "Error reading disk logs: " + ex.toString());
            ex.printStackTrace();
            return;
        }

        try {
            networkLogger.uploadPendingLogs(jsondata);
        } catch(Exception ex) {
            Log.d(TAG, "Unable to upload presently: " + ex.toString());
            ex.printStackTrace();
        }
    }
}
