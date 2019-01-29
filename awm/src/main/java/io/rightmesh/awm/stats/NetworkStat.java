package io.rightmesh.awm.stats;

import java.util.HashSet;
import java.util.Set;

public abstract class NetworkStat {

    String name;
    String mac;
    Set<String> macs = new HashSet<>();

    public int getSize() {
        return macs.size();
    }

    public String getMac() {
        return mac;
    }

    public String getName() { return name; }

    public Set<String> getMacs() {
        return macs;
    }
}
