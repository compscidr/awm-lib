package io.rightmesh.awm.collectors;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.util.HashSet;

import io.rightmesh.awm.stats.BluetoothStats;

public class BluetoothStatsCollector extends StatsCollector {

    private static final String TAG = BluetoothStatsCollector.class.getCanonicalName();

    private Context context;

    private static BluetoothAdapter mBluetoothAdapter;
    private static volatile BluetoothStates btState;
    private BluetoothBroadcastReceiver bluetoothBroadcastReceiver;

    private HashSet<BluetoothDevice> btDevices;

    enum BluetoothStates {
        ON, OFF, REJECTED
    }

    public BluetoothStatsCollector(Context context) {
        this.context = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothBroadcastReceiver = new BluetoothBroadcastReceiver();
        btDevices = new HashSet<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        context.registerReceiver(bluetoothBroadcastReceiver, filter);
    }

    @Override
    public void start() throws Exception {
        if(mBluetoothAdapter == null) {
            throw new Exception("Bluetooth adapter is null, likely not supported by phone");
        }

        //turn on BT if its not already on
        if(!mBluetoothAdapter.isEnabled()) {
            btState = BluetoothStates.OFF;
            Log.i(TAG, "Bluetooth off, requesting to turn on");
            context.startActivity(new Intent(context, BluetoothEnableActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            btState = BluetoothStates.ON;
            Log.i(TAG, "Bluetooth already on. Starting discovery");
            mBluetoothAdapter.startDiscovery();
        }
    }

    @Override
    public void stop() {
        //we may want to consider restoring the initial state of the bt device from before
        //the library started, ie if it was on, leave it on, if it was off, turn it back off.

        if(bluetoothBroadcastReceiver != null) {
            context.unregisterReceiver(bluetoothBroadcastReceiver);
            bluetoothBroadcastReceiver = null;
        }
    }

    public class BluetoothBroadcastReceiver extends BroadcastReceiver {

        private final String TAG = BluetoothBroadcastReceiver.class.getCanonicalName();

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.d(TAG, "Broadcast action is null, can't continue to process the intent");
                return;
            }

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) {
                    Log.d(TAG, "Device is null in the broadcastreceiver");
                } else {
                    Log.d(TAG, "Found: " + device.getName() + " " + device.getAddress());
                    btDevices.add(device);
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d(TAG, "After scan found a total of " + btDevices.size() + " devices");
                eventBus.post(new BluetoothStats(btDevices, mBluetoothAdapter.getAddress()));
                btDevices.clear();
                mBluetoothAdapter.startDiscovery();
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                Log.d(TAG, "Starting discovery scan");
            }
        }
    }

    /**
     * This class is so that we can capture the result of whether the user agreed to turn on the
     * bluetooth device.
     */
    public static class BluetoothEnableActivity extends Activity {

        private final String TAG = BluetoothEnableActivity.class.getCanonicalName();
        private static final int REQUEST_ENABLE_BT = 109;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Log.d(TAG, "BTENABLEACTIVITY");

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_ENABLE_BT) {
                Log.d(TAG, "REQUEST ENABLE BT RESULT: " + resultCode);
                if(mBluetoothAdapter.isEnabled()) {
                    Log.d(TAG, "Accepted turning on BT device. Starting discovery.");
                    btState = BluetoothStates.ON;
                    mBluetoothAdapter.startDiscovery();
                } else {
                    btState = BluetoothStates.REJECTED;
                }
                finish();
            } else {
                Log.d(TAG, "Unknown request code received by " + TAG + " activity");
            }
        }
    }
}
