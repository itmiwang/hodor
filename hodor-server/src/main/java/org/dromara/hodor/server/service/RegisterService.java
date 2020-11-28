package org.dromara.hodor.server.service;

import org.dromara.hodor.common.extension.ExtensionLoader;
import org.dromara.hodor.common.utils.GsonUtils;
import org.dromara.hodor.common.utils.LocalHost;
import org.dromara.hodor.core.CopySet;
import org.dromara.hodor.core.HodorMetadata;
import org.dromara.hodor.register.api.DataChangeListener;
import org.dromara.hodor.register.api.RegistryCenter;
import org.dromara.hodor.register.api.RegistryConfig;
import org.dromara.hodor.register.api.node.LeaderNode;
import org.dromara.hodor.register.api.node.ServerNode;
import org.dromara.hodor.server.component.LifecycleComponent;
import org.dromara.hodor.server.config.HodorServerProperties;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *  register service
 *
 * @author tomgs
 * @version 2020/6/29 1.0 
 */
@Service
public class RegisterService implements LifecycleComponent {

    private final RegistryCenter registryCenter;

    private final HodorServerProperties properties;

    private final GsonUtils gsonUtils;

    private final String serverId;

    public RegisterService(final HodorServerProperties properties) {
        this.properties = properties;
        this.registryCenter = ExtensionLoader.getExtensionLoader(RegistryCenter.class).getDefaultJoin();
        this.gsonUtils = GsonUtils.getInstance();
        this.serverId = LocalHost.getIp() + ":" + properties.getNetServerPort();
    }

    @Override
    public void start() {
        RegistryConfig config = RegistryConfig.builder().servers(properties.getRegistryServers()).namespace(properties.getRegistryNamespace()).build();
        registryCenter.init(config);
        initNode();
    }

    @Override
    public void stop() {
        registryCenter.close();
    }

    public RegistryCenter getRegistryCenter() {
        return registryCenter;
    }

    private void initNode() {
        // init path
        //registryCenter.makeDirs(ServerNode.METADATA_PATH);
        //registryCenter.makeDirs(ServerNode.NODES_PATH);
        //registryCenter.makeDirs(ServerNode.COPY_SETS_PATH);
        //registryCenter.makeDirs(ServerNode.MASTER_PATH);
        //registryCenter.makeDirs(ServerNode.WORK_PATH);

        // init data
        registryCenter.createPersistent(ServerNode.getServerNodePath(getServerId()), getServerId());

    }

    public Integer getRunningNodeCount() {
        return getRunningNodes().size();
    }

    public List<String> getRunningNodes() {
        return registryCenter.getChildren(ServerNode.NODES_PATH);
    }

    @Deprecated
    public void createCopySet(CopySet copySet) {
        String serversPath = registryCenter.makePath(ServerNode.COPY_SETS_PATH, String.valueOf(copySet.getId()), "servers");
        for (String server : copySet.getServers()) {
            registryCenter.createEphemeral(serversPath, server);
        }
    }

    public void createMetadata(HodorMetadata metadata) {
        registryCenter.createPersistent(ServerNode.METADATA_PATH, gsonUtils.toJson(metadata));
        //for (CopySet copyset : metadata.getCopySets()) {
        //    createCopySet(copyset);
        //}
    }

    public void registryMetadataListener(DataChangeListener listener) {
        registryListener(ServerNode.METADATA_PATH, listener);
    }

    public void registryServerNodeListener(DataChangeListener listener) {
        registryListener(ServerNode.NODES_PATH, listener);
    }

    public void registryElectLeaderListener(DataChangeListener listener) {
        registryListener(LeaderNode.ACTIVE_PATH, listener);
    }

    public void registryWorkerNodeListener(DataChangeListener listener) {
        registryListener(ServerNode.WORKER_PATH, listener);
    }

    public void registryListener(String path, DataChangeListener listener) {
        registryCenter.addDataCacheListener(path, listener);
    }

    public String getServerId() {
        return serverId;
    }

    public Integer getLeastNodeCount() {
        //return properties.getClusterNodes();
        return Integer.parseInt(System.getProperty("clusters", "1"));
    }

    public List<String> getAllWorkNodes(String groupName) {
        return registryCenter.getChildren(ServerNode.WORKER_PATH + "/" + groupName);
    }

}
