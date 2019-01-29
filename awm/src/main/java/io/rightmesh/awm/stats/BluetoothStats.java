package io.rightmesh.awm.stats;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.util.Collection;

import io.rightmesh.awm.stats.NetworkStat;

public class BluetoothStats extends NetworkStat implements Serializable {

    private static final long serialVersionUID = 1L;

    public BluetoothStats(Collection<BluetoothDevice> btDevices, String mac) {
        this.mac = mac;
        for(BluetoothDevice device : btDevices) {
            macs.add(device.getAddress());
        }
    }
}
