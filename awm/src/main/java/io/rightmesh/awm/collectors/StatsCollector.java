package io.rightmesh.awm.collectors;

import com.anadeainc.rxbus.Bus;
import com.anadeainc.rxbus.BusProvider;
import com.vanniktech.rxpermission.Permission;

import java.util.Map;

import io.rightmesh.awm.AwmComponent;

public abstract class StatsCollector implements AwmComponent {

    Bus eventBus = BusProvider.getInstance();
    Map<String, Permission> permissions = null;

    public void setPermissions(Map<String, Permission> permissions) {
        this.permissions = permissions;
    }

    public abstract void start() throws Exception;
    public abstract void stop();
}
