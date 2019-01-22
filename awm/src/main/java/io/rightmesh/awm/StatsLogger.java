package io.rightmesh.awm;

import android.content.Context;
import android.util.Log;

import com.anadeainc.rxbus.Bus;
import com.anadeainc.rxbus.BusProvider;
import com.anadeainc.rxbus.Subscribe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to send the stats to the remote server (or if it is unavailable because the
 * device is offline, caching the stats locally
 */
public class StatsLogger {

    private static final String TAG = StatsLogger.class.getCanonicalName();

    ScheduledExecutorService scheduleTaskExecutor;
    Bus eventBus = BusProvider.getInstance();
    GPSStats lastPosition = new GPSStats(0,0);
    BufferedWriter bufferedWriter;
    BufferedReader bufferedReader;
    Context context;

    public StatsLogger(Context context) {
        scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
        this.context = context;
    }

    void start() {
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                    context.openFileOutput("data.dat", Context.MODE_APPEND)));
            bufferedReader = new BufferedReader(new InputStreamReader(
                    context.openFileInput("data.dat")));
        } catch(IOException ex) {
            Log.d(TAG, "Failed to open files for logging: " + ex.toString());
            ex.printStackTrace();
        }
        eventBus.register(this);
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                uploadDisk();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    void stop() {
        eventBus.unregister(this);
        try {
            if(bufferedWriter != null) {
                bufferedWriter.close();
            }
            if(bufferedReader != null) {
                bufferedReader.close();
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    @Subscribe
    public void updateBTDevices(BluetoothStats btStats) {
        btStats.setPosition(lastPosition);

        //if the user doesn't give permission for storage, this could be null
        if(bufferedWriter != null) {
            try {
                logDisk(btStats);
            } catch (IOException ex) {
                Log.e(TAG, "Couldn't write to log file, measurement lost: " + ex.toString());
            }
        } else {
            logNetwork(btStats);
        }
    }

    @Subscribe
    public void updateWiFiStats(WiFiStats wiFiStats) {
        wiFiStats.setPosition(lastPosition);

        //if the user doesn't give permission for storage, this could be null
        if(bufferedWriter != null) {
            try {
                logDisk(wiFiStats);
            } catch (IOException ex) {
                Log.e(TAG, "Couldn't write to log file, measurement lost: " + ex.toString());
            }
        } else {
            logNetwork(wiFiStats);
        }
    }

    @Subscribe
    public void updateGPS(GPSStats gpsStats) {
        lastPosition = gpsStats;
    }

    /**
     * Used to log to disk in case the network fails
     */
    private void logDisk(NetworkStat networkStat) throws IOException {

        String outputString;
        if(networkStat instanceof BluetoothStats) {
            outputString = "  \t\"bt\": [\n";
        } else if (networkStat instanceof WiFiStats) {
            outputString = "  \t\"wifi\": [\n";
        } else {
            Log.e(TAG, "Unknown stats instance. Can't log to disk. ignoring");
            return;
        }
        outputString = outputString + "  \t\t[" + networkStat.getPosition().longitude
                + " , " + networkStat.getPosition().latitude + ", "
                + networkStat.getSize() + "]\n\t]\n";
        bufferedWriter.write(outputString);
        bufferedWriter.flush();
    }

    /**
     * Attempts to upload the saved stats to the server.
     */
    private void uploadDisk() {
        Runnable sendData = () -> {
            try {
                URL url = new URL("https://test.rightmesh.io/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                String jsondata = "{\n" +
                        "  \"stats\": {\n";

                String line = bufferedReader.readLine();
                while(line != null) {
                    jsondata = jsondata.concat(line);
                    line = bufferedReader.readLine();
                }

                jsondata = jsondata + "  }\n}\n";
                Log.d(TAG, "JSONDATA: " + jsondata);

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsondata);
                os.flush();
                os.close();
                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG", conn.getResponseMessage());
                conn.disconnect();


                /*
                bufferedWriter.close();
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                        context.openFileOutput("data.dat",0)));
                        */

            } catch(Exception ex) {
                Log.d(TAG, "Error uploading disk to the server: " + ex.toString());
            }
        };
        new Thread(sendData).start();
    }

    //todo: check if there is an internet connection first
    private void logNetwork(NetworkStat networkStat) {
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);

        Runnable sendData = () -> {
            try {
                URL url = new URL("https://test.rightmesh.io/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                String jsondata = "{\n" +
                        "  \"stats\": {\n";

                if(networkStat instanceof BluetoothStats) {
                    jsondata = jsondata + "  \t\"bt\": [\n";
                } else if (networkStat instanceof WiFiStats) {
                    jsondata = jsondata + "  \t\"wifi\": [\n";
                } else {
                    Log.d(TAG, "Unknown stats instance. ignoring");
                    return;
                }

                jsondata = jsondata + "  \t\t[" + networkStat.getPosition().longitude + " , " + networkStat.getPosition().latitude + ", " + networkStat.getSize() + "]\n" +
                        "  \t]\n" +
                        "  }\n" +
                        "}\n";

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsondata);
                os.flush();
                os.close();
                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG", conn.getResponseMessage());
                conn.disconnect();
            } catch(Exception ex) {
                //ex.printStackTrace();
            }
        };
        new Thread(sendData).start();
    }
}
