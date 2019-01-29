package io.rightmesh.awm.loggers;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import io.rightmesh.awm.ObservingDevice;
import io.rightmesh.awm.stats.BluetoothStats;
import io.rightmesh.awm.stats.NetworkStat;
import io.rightmesh.awm.stats.WiFiStats;

public class DiskLogger implements StatsLogger {

    private static final String TAG = DiskLogger.class.getCanonicalName();
    private Context context;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    public DiskLogger(Context context) {
        this.context = context;
    }

    @Override public void start() {
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                    context.openFileOutput("data.dat", Context.MODE_APPEND)));
            bufferedReader = new BufferedReader(new InputStreamReader(
                    context.openFileInput("data.dat")));
        } catch(IOException ex) {
            Log.d(TAG, "Failed to open files for logging: " + ex.toString());
            ex.printStackTrace();
        }
    }

    @Override public void stop() {
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

    @Override
    public void log(NetworkStat stat, ObservingDevice thisDevice) throws Exception {
        String outputString;
        if(stat instanceof BluetoothStats) {
            outputString = "  \t\"bt\": [\n";
        } else if (stat instanceof WiFiStats) {
            outputString = "  \t\"wifi\": [\n";
        } else {
            Log.e(TAG, "Unknown stats instance. Can't log to disk. ignoring");
            return;
        }
        outputString = outputString + "  \t\t[" + thisDevice.getPosition().longitude
                + " , " + thisDevice.getPosition().latitude + ", "
                + stat.getSize() + "]\n\t]\n";
        bufferedWriter.write(outputString);
        bufferedWriter.flush();
    }

    public String getPendingLogs() throws Exception {
        String jsondata = "{\n" +
                "  \"stats\": {\n";

        String line = bufferedReader.readLine();
        while(line != null) {
            jsondata = jsondata.concat(line);
            line = bufferedReader.readLine();
        }

        jsondata = jsondata + "  }\n}\n";
        Log.d(TAG, "JSONDATA: " + jsondata);

        return jsondata;
    }
}
