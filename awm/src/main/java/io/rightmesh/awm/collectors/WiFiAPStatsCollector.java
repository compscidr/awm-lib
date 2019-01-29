package io.rightmesh.awm.collectors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

import io.rightmesh.awm.stats.WiFiStats;

public class WiFiAPStatsCollector extends StatsCollector {

    private static final String TAG = WiFiAPStatsCollector.class.getCanonicalName();

    private Context context;
    private WifiManager wifiManager;
    private WifiManager.WifiLock wifiLock;
    private WiFiScanReceiver wiFiScanReceiver;

    public WiFiAPStatsCollector(Context context) {
        this.context = context;

        wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "AWM-LIB");
        wiFiScanReceiver = new WiFiScanReceiver();

    }

    @Override
    public void start() throws Exception {
        if(wifiLock != null) {
            wifiLock.acquire();
        }
        context.registerReceiver(wiFiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifiManager.startScan();
    }

    @Override
    public void stop() {
        if(wifiLock != null) {
            wifiLock.release();
        }
        context.unregisterReceiver(wiFiScanReceiver);
    }

    public class WiFiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //todo on android 8 and 9 we should check if these scans are new or not

            List<ScanResult> scans = wifiManager.getScanResults();
            if(scans.size() == 0) {
                Log.d(TAG, "Scan empty. Waiting for another OS scan or an app resume");
                //wifiManager.startScan();
            } else {
                Log.d(TAG, "GOT SCAN");
                for(ScanResult scan : scans) {
                    Log.d(TAG, scan.BSSID + " " + scan.SSID);
                }
                eventBus.post(new WiFiStats(scans, wifiManager.getConnectionInfo().getMacAddress()));
            }
        }
    }
}
