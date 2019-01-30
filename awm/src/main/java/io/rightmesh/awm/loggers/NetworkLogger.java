package io.rightmesh.awm.loggers;

import android.util.Log;

import com.anadeainc.rxbus.Bus;
import com.anadeainc.rxbus.BusProvider;
import com.anadeainc.rxbus.RxBus;

import java.io.DataOutputStream;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import io.rightmesh.awm.ObservingDevice;
import io.rightmesh.awm.stats.BluetoothStats;
import io.rightmesh.awm.stats.NetworkStat;
import io.rightmesh.awm.stats.WiFiStats;

public class NetworkLogger implements StatsLogger {

    private static final String TAG = NetworkLogger.class.getCanonicalName();
    private static final String DBURL = "https://test.rightmesh.io/awm-lib-server/";
    private Bus eventBus;

    public NetworkLogger() {
         eventBus = BusProvider.getInstance();
    }

    /**
     * Logs a single record from the network if internet is available.
     * @param stat
     * @param thisDevice
     * @throws Exception
     */
    @Override
    public void log(NetworkStat stat, ObservingDevice thisDevice) throws Exception {
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);

        Log.d(TAG, "LOGGING TO NETWORK!");

        Runnable sendData = () -> {
            try {
                URL url = new URL(DBURL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                String deviceJson = "";

                int count = 1;
                Set<String> macs = stat.getMacs();
                for(String mac : macs) {
                    deviceJson += "{\n";
                    deviceJson += "\t\"mac_address\": \"" + mac + "\",\n";
                    if(stat instanceof BluetoothStats) {
                        deviceJson += "\t\"mac_type\": " + 0 + ",\n";
                    } else if (stat instanceof WiFiStats) {
                        deviceJson += "\t\"mac_type\": " + 1 + ",\n";
                    } else {
                        deviceJson += "\t\"mac_type\": " + -1 + ",\n";
                    }
                    deviceJson += "\t\"network_name\": \"" + stat.getName() + "\"\n";
                    if(count < macs.size()) {
                        deviceJson += "},\n";
                    } else {
                        deviceJson += "}\n";
                    }
                    count++;
                }

                String jsondata = thisDevice.prepareJSON(deviceJson, new Timestamp(new Date().getTime()));
                Log.d(TAG, "JSONDATA: \n" + jsondata);

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsondata);
                os.flush();
                os.close();
                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG", conn.getResponseMessage());
                conn.disconnect();
            } catch(Exception ex) {
                Log.d(TAG, "Error sending to network: " + ex.toString());
                ex.printStackTrace();
            }
        };
        new Thread(sendData).start();
    }

    /**
     * Logs all saved records from disk
     */
    public void uploadPendingLogs(ArrayList<String> jsondata) {
        Runnable sendData = () -> {
            try {
                for(String data : jsondata) {
                    if(data.length() == 0) {
                        continue;
                    }
                    URL url = new URL(DBURL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(data);
                    os.flush();
                    os.close();
                    Log.i("UPLOAD PENINDING DATA", data);
                    Log.i("UPLOAD PENDING STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("UPLOAD PENDING MSG", conn.getResponseMessage());
                    conn.disconnect();

                    if(conn.getResponseCode() == 400) {
                        eventBus.post(new LogEvent(LogEvent.EventType.MALFORMED));
                        return;
                    }
                }
                eventBus.post(new LogEvent(LogEvent.EventType.SUCCESS));

            } catch(Exception ex) {
                Log.d(TAG, "Error uploading disk to the server: " + ex.toString());
                eventBus.post(new LogEvent(LogEvent.EventType.FAILURE));
            }
        };
        new Thread(sendData).start();
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() {

    }
}
