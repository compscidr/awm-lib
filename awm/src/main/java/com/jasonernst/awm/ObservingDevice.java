package com.jasonernst.awm;

import android.util.Log;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.UUID;
import com.jasonernst.awm.stats.GPSStats;
import lombok.Setter;

public class ObservingDevice {

    private UUID uuid;
    private GPSStats position;
    private String OS;
    private Inet4Address inet4Address;
    private Inet6Address inet6Address;
    private float battery_life = 100;

    @Setter
    private boolean hasCellularInternet = false;

    @Setter
    private boolean hasWiFiInternet = false;

    private float cellularThroughput = 0;
    private float wifiThroughput = 0;
    private int cellularPing = 0;
    private int wifiPing = 0;
    private String cellularOperator = "unknown";
    private int cellularNetworktype = 0;

    private static final String TAG = ObservingDevice.class.getCanonicalName();

    public ObservingDevice(UUID uuid, String OS) {
        try {
            inet4Address = (Inet4Address)Inet4Address.getByName("0.0.0.0");
            inet6Address = (Inet6Address)Inet6Address.getByName("::");
        } catch(UnknownHostException ex) {
            Log.d(TAG, "Unknown host: " + ex.toString());
        }
        this.position = new GPSStats(0,0);
        this.uuid = uuid;
        this.OS = OS;
    }

    public void updatePosition(GPSStats position) {
        if(position.longitude != 0 && position.latitude != 0) {
            this.position = position;
        } else {
            Log.d(TAG,"Leave last position since new one is zero");
        }
    }

    void setInet4Address(Inet4Address inet4Address) { this.inet4Address = inet4Address; }

    void setInet6Address(Inet6Address inet6Address) { this.inet6Address = inet6Address; }

    void setBattery(float battery) { this.battery_life = battery; }

    public GPSStats getPosition() {
        return position;
    }

    /**
     * Given the observing device position data, UUID, and mac addresses, this method will take
     * the device list which should be pre-prepared in JSON and create a fully JSON request
     * ready to be transmitted to the
     * @param deviceJSON
     * @param timestamp
     * @return
     */
    public String prepareJSON(String deviceJSON, Timestamp timestamp) {
        String json = "{\n";

        String longitude = String.format("%f", position.longitude);
        String latitude = String.format("%f", position.latitude);

        json += "\t\"awm_measure\": {\n";
        json += "\t\t\"reporting_device\": {\n";
        json += "\t\t\t\"uuid\": \"" + uuid.toString() + "\",\n";
        json += "\t\t\t\"ipv4_address\": \"" + inet4Address.getHostAddress() + "\",\n";
        json += "\t\t\t\"ipv6_address\": \"" + inet6Address.getHostAddress() + "\",\n";
        json += "\t\t\t\"timestamp\": \"" + timestamp.toString() + "\",\n";
        json += "\t\t\t\"longitude\": \"" + longitude + "\",\n";
        json += "\t\t\t\"latitude\": \"" + latitude + "\",\n";
        json += "\t\t\t\"bt_mac_address\": \"00:00:00:00:00\",\n";
        json += "\t\t\t\"wifi_mac_address\": \"00:00:00:00:00\",\n";
        json += "\t\t\t\"OS\": \"" + OS + "\",\n";
        json += "\t\t\t\"battery_life\": \"" + battery_life + "\",\n";
        json += "\t\t\t\"has_cellular_internet\": \"" + hasCellularInternet + "\",\n";
        json += "\t\t\t\"has_wifi_internet\": \"" + hasWiFiInternet + "\",\n";
        json += "\t\t\t\"cellular_throughput\": \"" + cellularThroughput + "\",\n";
        json += "\t\t\t\"wifi_throughput\": \"" + wifiThroughput + "\",\n";
        json += "\t\t\t\"cellular_ping\": \"" + cellularPing + "\",\n";
        json += "\t\t\t\"wifi_ping\": \"" + wifiPing + "\",\n";
        json += "\t\t\t\"cellular_operator\": \"" + cellularOperator + "\",\n";
        json += "\t\t\t\"cellular_network_type\": \"" + cellularNetworktype + "\"\n";
        json += "\t\t},\n";
        json += "\t\t\"devices\": [\n";

        json += deviceJSON;

        json += "\t\t]\n";
        json += "\t}\n";
        json += "}\n";

        return json;
    }
}
