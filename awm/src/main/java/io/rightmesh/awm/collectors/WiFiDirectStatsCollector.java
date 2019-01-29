package io.rightmesh.awm.collectors;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import static android.os.Looper.getMainLooper;

public class WiFiDirectStatsCollector extends StatsCollector {

    private WifiP2pManager wifiDirectManager;
    private WifiP2pManager.Channel wifiDirectChannel;
    private Context context;
    private WiFiDirectScanReceiver wiFiDirectScanReceiver;
    private WiFiDirectChannelDisconnectListener wiFiDirectChannelDisconnectListener;

    public WiFiDirectStatsCollector(Context context) {
        this.context = context;
        wifiDirectManager = (WifiP2pManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_P2P_SERVICE);

        wiFiDirectChannelDisconnectListener = new WiFiDirectChannelDisconnectListener();
    }

    @Override
    public void start() throws Exception {
        //can be null if the device doesn't support wifi p2p (or wifi direct)
        if (wifiDirectManager == null) {
            throw new Exception("Device doesn't support Wi-Fi Direct");
        }

        //note: this call requires ACCESS_WIFI_STATE and CHANGE_WIFI_STATE
        wifiDirectChannel = wifiDirectManager.initialize(context.getApplicationContext(),
                getMainLooper(), wiFiDirectChannelDisconnectListener);

        wiFiDirectScanReceiver = new WiFiDirectScanReceiver();

        //WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance();
        //wifiDirectManager.addLocalService(wifiDirectChannel, "awm-lib", "awm-lib._udp", );
        wifiDirectManager.addServiceRequest(wifiDirectChannel, WifiP2pDnsSdServiceRequest.newInstance(), null);
        wifiDirectManager.setDnsSdResponseListeners(wifiDirectChannel, wiFiDirectScanReceiver, null);
    }

    @Override
    public void stop() {

    }

    public class WiFiDirectChannelDisconnectListener implements WifiP2pManager.ChannelListener {

        private final String TAG = WiFiDirectChannelDisconnectListener.class.getCanonicalName();

        @Override
        public void onChannelDisconnected() {
            Log.d(TAG, "Wi-Fi direct disconnected from comms framework.");
            wifiDirectChannel = wifiDirectManager.initialize(context.getApplicationContext(),
                    getMainLooper(), wiFiDirectChannelDisconnectListener);
        }
    }

    public class WiFiDirectScanReceiver implements WifiP2pManager.DnsSdServiceResponseListener {

        private final String TAG = WiFiDirectScanReceiver.class.getCanonicalName();

        @Override
        public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
            Log.d(TAG, "GOT A WIFI D DEVICE");
        }
    }
}
