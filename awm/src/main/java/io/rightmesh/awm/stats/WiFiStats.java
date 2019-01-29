package io.rightmesh.awm.stats;

import android.net.wifi.ScanResult;

import java.util.Collection;

import io.rightmesh.awm.stats.NetworkStat;

public class WiFiStats extends NetworkStat {

    public WiFiStats(Collection<ScanResult> wifiDevices, String mac) {
        this.mac = mac;
        for(ScanResult scan : wifiDevices) {
            macs.add(scan.BSSID);
        }

    }
}
