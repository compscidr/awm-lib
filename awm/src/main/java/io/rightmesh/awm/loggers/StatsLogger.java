package io.rightmesh.awm.loggers;

import io.rightmesh.awm.AwmComponent;
import io.rightmesh.awm.ObservingDevice;
import io.rightmesh.awm.stats.NetworkStat;

public interface StatsLogger extends AwmComponent {
    void log(NetworkStat stat, ObservingDevice thisDevice) throws Exception;
}
